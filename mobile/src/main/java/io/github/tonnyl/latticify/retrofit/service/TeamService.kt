package io.github.tonnyl.latticify.retrofit.service

import io.github.tonnyl.latticify.data.TeamIntegrationLogsWrapper
import io.github.tonnyl.latticify.data.TeamLogsWrapper
import io.github.tonnyl.latticify.data.TeamWrapper
import io.github.tonnyl.latticify.retrofit.Api
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Created by lizhaotailang on 08/10/2017.
 */
interface TeamService {

    /**
     * Gets the access logs for the current team.
     * This is only available to paid teams.
     *
     * @param token Required. Authentication token bearing required scopes.
     * @param before Optional, default=now. End of time range of logs to include in results (inclusive). e.g. 1457989166.
     * @param count Optional, default=100. Number of items to return per page.
     * @param page Optional, default=1. Page number of results to return.
     *
     * @return If successful, the command returns a list of logins followed by pagination information.
     */
    @POST("team.accessLogs")
    @FormUrlEncoded
    fun accessLogs(@Field("token") token: String,
                   @Field("before") before: String = "0",
                   @Field("count") count: Int = 20,
                   @Field("page") page: Int = 1): Observable<TeamLogsWrapper>

    /**
     * Gets information about the current team.
     *
     * @param token Required. Authentication token bearing required scopes.
     *
     * @return If successful, the command returns a [TeamWrapper] object.
     */
    @POST("team.info")
    @FormUrlEncoded
    fun info(@Field("token") token: String): Observable<TeamWrapper>

    /**
     * Gets the integration logs for the current team.
     *
     * @param token Required. Authentication token bearing required scopes.
     * @param appId Optional. Filter logs to this Slack app. Defaults to all logs.
     * @param changeType Optional. Filter logs with this change type. Defaults to all logs.
     * See [io.github.tonnyl.latticify.data.TeamIntegrationLog.changeType] for all the types.
     * @param count Optional, default=100. Number of items to return per page.
     * @param page Optional, default=1. Page number of results to return.
     * @param serviceId Optional. Filter logs to this service. Defaults to all logs.
     * @param userId Optional. Filter logs generated by this user’s actions. Defaults to all logs.
     *
     * @return If successful, the command returns a [TeamIntegrationLogsWrapper] object.
     */
    @POST("team.integrationLogs")
    @FormUrlEncoded
    fun integrationLogs(@Field("token") token: String,
                        @Field("app_id") appId: String = "",
                        @Field("change_type") changeType: String = "",
                        @Field("count") count: Int = Api.LIMIT,
                        @Field("page") page: Int = 1,
                        @Field("service_id") serviceId: String = "",
                        @Field("user") userId: String): Observable<TeamIntegrationLogsWrapper>

}