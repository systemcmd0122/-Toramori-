package com.example.e_zuka.data.region

import android.util.Log
import com.example.e_zuka.data.model.RegionAuthResult
import com.example.e_zuka.data.model.RegionData
import com.example.e_zuka.data.model.UserRegionData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RegionAuthRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "RegionAuthRepository"
        private const val REGION_CODES_COLLECTION = "regionCodes"
        private const val USER_REGIONS_COLLECTION = "userRegions"
    }

    suspend fun getUserRegionData(userId: String): UserRegionData? {
        return try {
            Log.d(TAG, "Getting user region data for: $userId")

            val userRegionDoc = firestore.collection(USER_REGIONS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (userRegionDoc.exists()) {
                val userData = userRegionDoc.toObject(UserRegionData::class.java)
                if (userData?.isVerified == true &&
                    userData.regionCodeId.isNotBlank() &&
                    userData.regionName.isNotBlank()
                ) {
                    Log.d(TAG, "Valid user region data found for $userId - verified: true, region: ${userData.regionName}")
                    userData
                } else {
                    Log.d(TAG, "Invalid or incomplete user region data found for $userId")
                    null
                }
            } else {
                Log.d(TAG, "No user region data found for user: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user region data for user: $userId", e)
            null
        }
    }

    suspend fun verifyRegionCode(code: String, userId: String): RegionAuthResult {
        return try {
            Log.d(TAG, "Starting region code verification for user: $userId, code: $code")

            // 既に認証済みかチェック
            val existingVerification = checkExistingVerification(userId)
            if (existingVerification != null) {
                Log.d(TAG, "User already has region verification: ${existingVerification.regionName}")
                return RegionAuthResult(
                    success = true,
                    regionData = RegionData(
                        codeId = existingVerification.regionCodeId,
                        regionName = existingVerification.regionName,
                        createdAt = existingVerification.verifiedAt
                    )
                )
            }

            // 地域認証コードを検索
            val regionQuery = firestore.collection(REGION_CODES_COLLECTION)
                .whereEqualTo("code", code.uppercase().trim())
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()

            if (regionQuery.isEmpty) {
                Log.w(TAG, "Invalid region code: $code")
                return RegionAuthResult(
                    success = false,
                    errorMessage = "無効な地域認証コードです"
                )
            }

            val regionDoc = regionQuery.documents[0]
            val regionData = regionDoc.toObject(RegionData::class.java)?.copy(
                codeId = regionDoc.id
            ) ?: return RegionAuthResult(
                success = false,
                errorMessage = "地域データの取得に失敗しました"
            )

            Log.d(TAG, "Found region data: ${regionData.regionName}")

            // ユーザーの地域認証情報を保存
            val userRegionData = UserRegionData(
                userId = userId,
                regionCodeId = regionData.codeId,
                regionName = regionData.regionName,
                verifiedAt = Timestamp.now(),
                isVerified = true
            )

            firestore.collection(USER_REGIONS_COLLECTION)
                .document(userId)
                .set(userRegionData)
                .await()

            Log.d(TAG, "User region data saved successfully")

            // 使用回数を更新
            regionDoc.reference.update(
                "currentUsageCount",
                regionData.currentUsageCount + 1
            ).await()

            Log.d(TAG, "Region verification successful for user: $userId, region: ${regionData.regionName}")

            RegionAuthResult(
                success = true,
                regionData = regionData
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify region code", e)
            RegionAuthResult(
                success = false,
                errorMessage = "地域認証の処理中にエラーが発生しました: ${e.message}"
            )
        }
    }

    private suspend fun checkExistingVerification(userId: String): UserRegionData? {
        return try {
            Log.d(TAG, "Checking existing verification for user: $userId")

            val userRegionDoc = firestore.collection(USER_REGIONS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (userRegionDoc.exists()) {
                val userData = userRegionDoc.toObject(UserRegionData::class.java)
                if (userData?.isVerified == true &&
                    userData.regionCodeId.isNotBlank() &&
                    userData.regionName.isNotBlank()
                ) {
                    Log.d(TAG, "Found valid existing verification: ${userData.regionName}")
                    userData
                } else {
                    Log.d(TAG, "Invalid existing verification data found")
                    null
                }
            } else {
                Log.d(TAG, "No existing verification document found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check existing verification", e)
            null
        }
    }

    suspend fun resetRegionVerification(userId: String): RegionAuthResult {
        return try {
            Log.d(TAG, "Resetting region verification for user: $userId")

            firestore.collection(USER_REGIONS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            Log.d(TAG, "Region verification reset successful for user: $userId")

            RegionAuthResult(success = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset region verification for user: $userId", e)
            RegionAuthResult(
                success = false,
                errorMessage = "地域認証のリセットに失敗しました: ${e.message}"
            )
        }
    }

    suspend fun getActiveRegionCodes(): List<RegionData> {
        return try {
            Log.d(TAG, "Getting active region codes")

            val snapshot = firestore.collection(REGION_CODES_COLLECTION)
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val regionCodes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RegionData::class.java)?.copy(
                    codeId = doc.id
                )
            }

            Log.d(TAG, "Found ${regionCodes.size} active region codes")

            regionCodes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active region codes", e)
            emptyList()
        }
    }
}