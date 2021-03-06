/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.siddhi.service.api;

import io.siddhi.service.model.Success;
import io.swagger.annotations.ApiParam;
import io.siddhi.service.factories.SiddhiApiServiceFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/siddhi")

@io.swagger.annotations.Api(description = "The siddhi API")
public class SiddhiApi {
    private final SiddhiApiService delegate = SiddhiApiServiceFactory.getSiddhiApi();

    @POST
    @Path("/artifact/deploy")
    @Consumes({"text/plain"})
    @Produces({"application/json"})
    @io.swagger.annotations.ApiOperation(value = "", notes = "Deploys the siddhi app. Request **SiddhiApp** " +
            "explains the Siddhi Query ", response = Success.class, tags = {})
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Successful response", response = Success.class),
            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Success.class)})
    public Response siddhiArtifactDeployPost(@ApiParam(value = "Siddhi Siddhi app", required = true) String body)
            throws NotFoundException {
        return delegate.siddhiArtifactDeployPost(body);
    }

    @GET
    @Path("/artifact/undeploy/{siddhiApp}")
    @Produces({"application/json"})
    @io.swagger.annotations.ApiOperation(value = "", notes = "Undeploys the siddhi app as given by " +
            "`siddhiAppName`. Path param of **siddhiAppName** determines name of the siddhi app ",
            response = Success.class, tags = {})
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Successful response", response = Success.class),
            @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Success.class)})
    public Response siddhiArtifactUndeploySiddhiAppGet(
            @ApiParam(value = "Siddhi app Name", required = true) @PathParam("siddhiApp") String siddhiApp)
            throws NotFoundException {
        return delegate.siddhiArtifactUndeploySiddhiAppGet(siddhiApp);
    }
}
