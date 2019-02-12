/*
 * Copyright 2018-Present Platform Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kuraun.aws.maven.plugin.aws;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class AWSMavenCredentialsProvider implements AwsCredentialsProvider {

    private final AuthenticationInfo authenticationInfo;

    public AWSMavenCredentialsProvider(AuthenticationInfo authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return this.authenticationInfo != null ? new AWSMavenCredentials(this.authenticationInfo) : null;
    }
}
