package io.github.kuraun.aws.maven.plugin.aws;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class AWSMavenCredentialsProviderTest {

    @Test
    public void getCredentialsIfNull() {
        // GIVEN
        AWSMavenCredentialsProvider provider = new AWSMavenCredentialsProvider(null);

        // WHEN
        AwsCredentials actual = provider.resolveCredentials();

        // THEN
        assertThat(actual, nullValue());
    }

    @Test
    public void getCredentials() {
        // GIVEN
        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        authenticationInfo.setUserName("username");
        authenticationInfo.setPassword("password");
        AWSMavenCredentialsProvider provider = new AWSMavenCredentialsProvider(authenticationInfo);

        // WHEN
        AwsCredentials actual = provider.resolveCredentials();

        // THEN
        assertThat(actual.accessKeyId(), equalTo(authenticationInfo.getUserName()));
        assertThat(actual.secretAccessKey(), equalTo(authenticationInfo.getPassword()));
    }
}