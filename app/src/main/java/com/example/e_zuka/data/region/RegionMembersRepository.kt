package com.example.e_zuka.data.region

import android.util.Log
import com.example.e_zuka.data.model.RegionMemberData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@Suppress("UNCHECKED_CAST")
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