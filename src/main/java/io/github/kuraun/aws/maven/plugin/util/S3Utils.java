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

package io.github.kuraun.aws.maven.plugin.util;

import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.*;

public abstract class S3Utils {

    public static String getBucketName(Repository repository) {
        Objects.requireNonNull(repository, "repository must not be null");
        return repository.getHost();
    }

    public static String getBaseDirectory(Repository repository) {
        Objects.requireNonNull(repository, "repository must not be null");
        String basedir = substringAfter(repository.getBasedir(), "/");

        return isNotBlank(basedir) ? appendIfMissing(basedir, "/", "/") : basedir;
    }

    public static ApacheHttpClient.Builder getApacheHttpClientBuilder(ProxyInfoProvider proxyInfoProvider) {
        if (proxyInfoProvider != null) {
            ProxyInfo proxyInfo = proxyInfoProvider.getProxyInfo("s3");
            if (proxyInfo != null) {
                ProxyConfiguration.Builder proxyConfig =
                        ProxyConfiguration.builder().endpoint(URI.create("http://" + proxyInfo.getHost() + ":" + proxyInfo.getPort()));
                return ApacheHttpClient.builder()
                        .proxyConfiguration(proxyConfig.build());
            }
        }

        return ApacheHttpClient.builder();
    }
}
