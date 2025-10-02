package student.projects.tholagig.network

import retrofit2.Call
import retrofit2.http.*
import student.projects.tholagig.models.User

interface UserApiService {

    // Get user profile by ID
    @GET("users/{userId}")
    fun getUserProfile(@Path("userId") userId: String): Call<User>

    // Update user profile
    @PUT("users/{userId}")
    fun updateUserProfile(
        @Path("userId") userId: String,
        @Body user: User
    ): Call<User>
}