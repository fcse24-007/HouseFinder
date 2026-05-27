package com.example.housefinder.db.entities

import com.example.housefinder.db.entities.*
import org.mindrot.jbcrypt.BCrypt

object DatabaseInitializer {

    fun hashPassword(input: String): String = BCrypt.hashpw(input, BCrypt.gensalt())

    suspend fun seedUsers(db: AppDatabase) {
        val universities = GABORONE_UNIVERSITIES
        val names = listOf(
            "Kagiso Sithole", "Mpho Mokoena", "Tebogo Dlamini", "Naledi Kgosi",
            "Oratile Sebego", "Goabaone Tladi", "Kefilwe Molefe", "Thabo Nkwe",
            "Lesedi Gabaake", "Boipelo Marumo", "Ditiro Segwabe", "Onkutlwe Motlhale",
            "Sethunya Ramasedi", "Phenyo Keatlaretse", "Gofaone Mogopodi",
            "Refilwe Modise", "Tuelo Moseki", "Khumoetsile Lekwape", "Ipeleng Mosweu",
            "Baleseng Ntshole", "Modiri Chiepe", "Gaone Ramothwa", "Lorato Sadie",
            "Tlotlo Seretse", "Kenanao Gaboeletswe", "Mompati Mothibi", "Otsile Moroke",
            "Laone Garekwe", "Kefiloe Mosalakae", "Tshepiso Dingalo", "Kago Segaetsho",
            "Bontleng Molefi", "Lerato Kabelo", "Tshepo Rapetswa", "Sethabo Sengwe",
            "Ntombi Garekwe", "Motlapele Digang", "Pako Pule", "Kelebogile Mathe",
            "Gape Mogami", "Itumeleng Baaitse", "Maipelo Tlhomedi", "Kagiso Ramorobi",
            "Neo Gaetsewe", "Thatayaone Sello", "Gaositwe Kenosi", "Mmoloki Tau",
            "Thuli Dineo", "Rudo Matshediso", "Bokang Motshwari"
        )

        val studentNames = if (names.size >= SeedConfig.TARGET_STUDENT_COUNT) {
            names.take(SeedConfig.TARGET_STUDENT_COUNT)
        } else {
            names + (names.size + 1..SeedConfig.TARGET_STUDENT_COUNT).map { "Student $it" }
        }

        val maxStudentSequenceNumber = 900
        val yearCodeCycleSize = 30

        val users = studentNames.mapIndexed { i, name ->
            val config = universities[i % universities.size]
            val idx = ((i % maxStudentSequenceNumber) + 1).toString().padStart(3, '0')
            val yearCode = ((i % yearCodeCycleSize) + 1).toString().padStart(2, '0')
            val firstName = name.split(" ")[0].lowercase()

            User(
                studentId = "${config.idPrefix}${yearCode}-${idx}",
                name = name,
                email = "$firstName$idx@${config.emailDomain}",
                passwordHash = hashPassword("password$idx"),
                role = "STUDENT",
                university = config.name
            )
        }

        val providers = listOf(
            User(
                studentId = "PRV001",
                name = "Modise Properties",
                email = "modise@provider.bw",
                passwordHash = hashPassword("provider123"),
                role = "PROVIDER",
                university = "N/A"
            ),
            User(
                studentId = "PRV002",
                name = "Gaborone Student Homes",
                email = "gsh@provider.bw",
                passwordHash = hashPassword("provider456"),
                role = "PROVIDER",
                university = "N/A"
            )
        )

        db.userDao().insertAll(users + providers)
    }

    suspend fun seedListings(db: AppDatabase) {
        val locations = ListingCatalog.gaboroneAreas
        val types = ListingCatalog.supportedRoomTypes
        val amenitySets = listOf(
            """["Free WiFi","Study Desk","24/7 Security","Laundry"]""",
            """["Free WiFi","En-suite Bathroom","Study Desk"]""",
            """["24/7 Security","Shared Kitchen","Laundry"]"""
        )
        val availabilityDates = listOf("2024-08-01", "2024-08-15", "2024-09-01")
        val prices = listOf(950f, 1200f, 1500f, 1800f, 2200f)

        val listingTypes = if (types.size >= SeedConfig.TARGET_LISTING_COUNT) {
            types.take(SeedConfig.TARGET_LISTING_COUNT)
        } else {
            generateSequence(0) { it + 1 }
                .map { types[it % types.size] }
                .take(SeedConfig.TARGET_LISTING_COUNT)
                .toList()
        }

        val listings = (1..SeedConfig.TARGET_LISTING_COUNT).map { i ->
            val price = prices[i % prices.size]
            val type = listingTypes[i - 1]
            Listing(
                providerId = if (i <= 25) 51 else 52,
                title = "${locations[i % locations.size]} ${ListingCatalog.toDisplayType(type)}",
                description = "Quality student accommodation in Gaborone. Close to transport and shops.",
                price = price,
                location = locations[i % locations.size],
                type = type,
                amenities = amenitySets[i % amenitySets.size],
                depositAmount = price.toInt(),
                availabilityDate = availabilityDates[i % availabilityDates.size],
                status = "AVAILABLE",
                distanceToCampusKm = (0.5 + (i % 5)).toFloat()
            )
        }

        db.listingDao().insertAll(listings)

        val imageDrawables = listOf("listing_photo_1", "listing_photo_2", "listing_photo_3")
        val images = (1..SeedConfig.TARGET_LISTING_COUNT).flatMap { listingId ->
            imageDrawables.mapIndexed { index, drawable ->
                ListingImage(
                    listingId = listingId,
                    imagePath = drawable,
                    sortOrder = index
                )
            }
        }
        db.listingImageDao().insertAll(images)
    }

    suspend fun seedChatMessages(db: AppDatabase) {
        val initialMessages = listOf(
            ChatMessage(
                id = "seed-msg-1",
                conversationId = conversationIdFor(1, 51, 1),
                senderId = 1,
                receiverId = 51,
                listingId = 1,
                message = "Hi, is this room still available?",
                isRead = true,
                timestamp = System.currentTimeMillis() - 172_800_000L
            ),
            ChatMessage(
                id = "seed-msg-2",
                conversationId = conversationIdFor(1, 51, 1),
                senderId = 51,
                receiverId = 1,
                listingId = 1,
                message = "Yes, it is available from next month.",
                isRead = false,
                timestamp = System.currentTimeMillis() - 172_700_000L
            ),
            ChatMessage(
                id = "seed-msg-3",
                conversationId = conversationIdFor(2, 52, 26),
                senderId = 2,
                receiverId = 52,
                listingId = 26,
                message = "Can I schedule a viewing this weekend?",
                isRead = true,
                timestamp = System.currentTimeMillis() - 86_400_000L
            )
        )
        db.chatMessageDao().insertAll(initialMessages)
    }
}
