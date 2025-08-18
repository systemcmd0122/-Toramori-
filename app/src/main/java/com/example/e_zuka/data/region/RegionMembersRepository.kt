package com.example.e_zuka.data.region

import android.util.Log
import com.example.e_zuka.data.model.RegionMemberData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RegionMembersRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "RegionMembersRepository"
        private const val USER_REGIONS_COLLECTION = "userRegions"
        private const val USERS_COLLECTION = "users"
    }

    suspend fun getRegionMembers(regionCodeId: String): List<RegionMemberData> {
        return try {
            Log.d(TAG, "Getting region members for regionCodeId: $regionCodeId")

            // 指定された地域コードで認証済みのユーザーを取得
            val userRegionsQuery = firestore.collection(USER_REGIONS_COLLECTION)
                .whereEqualTo("regionCodeId", regionCodeId)
                .whereEqualTo("verified", true)
                .orderBy("verifiedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d(TAG, "Found ${userRegionsQuery.documents.size} verified users in region")

            val regionMembers = mutableListOf<RegionMemberData>()

            for (userRegionDoc in userRegionsQuery.documents) {
                try {
                    val userId = userRegionDoc.getString("userId") ?: continue
                    val regionName = userRegionDoc.getString("regionName") ?: ""
                    val verifiedAt = userRegionDoc.getTimestamp("verifiedAt") ?: continue

                    Log.d(TAG, "Processing user: $userId")

                    // ユーザーの詳細情報を取得
                    val userDoc = firestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .get()
                        .await()

                    val displayName = if (userDoc.exists()) {
                        userDoc.getString("displayName") ?: "名前未設定"
                    } else {
                        "名前未設定"
                    }

                    val member = RegionMemberData(
                        userId = userId,
                        displayName = displayName,
                        regionName = regionName,
                        joinedAt = verifiedAt
                    )

                    regionMembers.add(member)
                    Log.d(TAG, "Added member: ${member.displayName} (${member.userId})")

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process user region document", e)
                    // 個別のエラーは無視して続行
                }
            }

            Log.d(TAG, "Successfully processed ${regionMembers.size} members")

            regionMembers

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get region members", e)
            throw e
        }
    }

    suspend fun getMemberDetails(userId: String): RegionMemberData? {
        return try {
            Log.d(TAG, "Getting member details for userId: $userId")

            // ユーザーの地域認証情報を取得
            val userRegionDoc = firestore.collection(USER_REGIONS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (!userRegionDoc.exists()) {
                Log.d(TAG, "No region data found for user: $userId")
                return null
            }

            val regionName = userRegionDoc.getString("regionName") ?: ""
            val verifiedAt = userRegionDoc.getTimestamp("verifiedAt") ?: return null
            val isVerified = userRegionDoc.getBoolean("verified") ?: false

            if (!isVerified) {
                Log.d(TAG, "User is not verified: $userId")
                return null
            }

            // ユーザーの詳細情報を取得
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            val displayName = if (userDoc.exists()) {
                userDoc.getString("displayName") ?: "名前未設定"
            } else {
                "名前未設定"
            }

            val member = RegionMemberData(
                userId = userId,
                displayName = displayName,
                regionName = regionName,
                joinedAt = verifiedAt
            )

            Log.d(TAG, "Successfully got member details: ${member.displayName}")

            member

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get member details for user: $userId", e)
            null
        }
    }

    suspend fun getRegionMemberCount(regionCodeId: String): Int {
        return try {
            Log.d(TAG, "Getting member count for regionCodeId: $regionCodeId")

            val userRegionsQuery = firestore.collection(USER_REGIONS_COLLECTION)
                .whereEqualTo("regionCodeId", regionCodeId)
                .whereEqualTo("verified", true)
                .get()
                .await()

            val count = userRegionsQuery.documents.size
            Log.d(TAG, "Region member count: $count")

            count

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get region member count", e)
            0
        }
    }

    suspend fun isUserInRegion(userId: String, regionCodeId: String): Boolean {
        return try {
            Log.d(TAG, "Checking if user $userId is in region $regionCodeId")

            val userRegionDoc = firestore.collection(USER_REGIONS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (!userRegionDoc.exists()) {
                Log.d(TAG, "No region data found for user: $userId")
                return false
            }

            val userRegionCodeId = userRegionDoc.getString("regionCodeId") ?: ""
            val isVerified = userRegionDoc.getBoolean("verified") ?: false

            val result = isVerified && userRegionCodeId == regionCodeId
            Log.d(TAG, "User $userId in region $regionCodeId: $result")

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if user is in region", e)
            false
        }
    }

    suspend fun searchMembersInRegion(regionCodeId: String, searchQuery: String): List<RegionMemberData> {
        return try {
            Log.d(TAG, "Searching members in region $regionCodeId with query: $searchQuery")

            val allMembers = getRegionMembers(regionCodeId)

            val filteredMembers = if (searchQuery.isBlank()) {
                allMembers
            } else {
                allMembers.filter { member ->
                    member.displayName.contains(searchQuery, ignoreCase = true)
                }
            }

            Log.d(TAG, "Search found ${filteredMembers.size} members matching query")

            filteredMembers

        } catch (e: Exception) {
            Log.e(TAG, "Failed to search members in region", e)
            emptyList()
        }
    }

    suspend fun getUserSkills(userId: String): List<String> {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            if (userDoc.exists()) {
                userDoc.get("skills") as? List<String> ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user skills", e)
            emptyList()
        }
    }
}