/*
 * Copyright 2019-Present Kuraun Developers.
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

package io.github.kuraun.aws.maven.plugin;

import io.github.kuraun.aws.maven.plugin.data.TransferProgress;
import io.github.kuraun.aws.maven.plugin.data.transfer.TransferProgressFileInputStream;
import io.github.kuraun.aws.maven.plugin.data.transfer.TransferProgressFileOutputStream;
import io.github.kuraun.aws.maven.plugin.maven.AbstractWagon;
import io.github.kuraun.aws.maven.plugin.util.IOUtils;
import io.github.kuraun.aws.maven.plugin.util.S3Utils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.CollectionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of the Maven Wagon interface that allows you to access the Amazon S3 service. URLs that reference
 * the S3 service should be in the form of <code>s3://bucket.name</code>. As an example
 * <code>s3://static.springframework.org</code> would put files into the <code>static.springframework.org</code> bucket
 * on the S3 service.
 * <p>
 * This implementation uses the <code>username</code> and <code>passphrase</code> portions of the server authentication
 * metadata for credentials.
 */
public final class AmazonS3Wagon extends AbstractWagon {

    private static final String KEY_FORMAT = "%s%s";

    private static final String RESOURCE_FORMAT = "%s(.*)";

    private volatile S3Client amazonS3;

    private volatile String bucketName;

    private volatile String baseDirectory;

    /**
     * Creates a new instance of the wagon
     */
    public AmazonS3Wagon() {
        super(true);
    }

    AmazonS3Wagon(S3Client amazonS3, String bucketName, String baseDirectory) {
        super(true);
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.baseDirectory = baseDirectory;
    }

    private HeadObjectResponse getObjectMetadata(S3Client amazonS3,
            String bucketName, String baseDirectory, String resourceName) {
        return amazonS3.headObject(
                HeadObjectRequest.builder().bucket(bucketName)
                        .key(getKey(baseDirectory, resourceName)).build());
    }

    private static String getKey(String baseDirectory, String resourceName) {
        return String.format(KEY_FORMAT, baseDirectory, resourceName);
    }

    private static List<String> getResourceNames(ListObjectsResponse objectListing, Pattern pattern) {
        List<String> resourceNames = new ArrayList<>();

        for (CommonPrefix commonPrefix : objectListing.commonPrefixes()) {
            resourceNames.add(getResourceName(commonPrefix.prefix(), pattern));
        }

        for (S3Object s3ObjectSummary : objectListing.contents()) {
            resourceNames.add(getResourceName(s3ObjectSummary.key(), pattern));
        }

        return resourceNames;
    }

    private static String getResourceName(String key, Pattern pattern) {
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return key;
    }

    private static void mkdirs(S3Client amazonS3, String bucketName, String path, int index) throws TransferFailedException {
        int directoryIndex = path.indexOf('/', index) + 1;

        if (directoryIndex != 0) {
            String directory = path.substring(0, directoryIndex);
            try {
                amazonS3.putObject(PutObjectRequest.builder().bucket(bucketName)
                        .key(directory).build(), RequestBody
                        .fromByteBuffer(getRandomByteBuffer(10_000)));
            } catch (AwsServiceException | IOException e) {
                throw new TransferFailedException(String.format("Cannot write directory '%s'", directory), e);
            }

            mkdirs(amazonS3, bucketName, path, directoryIndex);
        }
    }

    private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
        byte[] b = new byte[size];
        new Random().nextBytes(b);
        return ByteBuffer.wrap(b);
    }

    private static String getBucketRegion(AwsCredentialsProvider credentialsProvider, ApacheHttpClient.Builder httpClientBuilder, String bucketName) {
        return S3Client.builder()
                // TODO: overrideClientConfiguration
                .credentialsProvider(credentialsProvider)
                .httpClientBuilder(httpClientBuilder).build().getBucketLocation(
                        GetBucketLocationRequest.builder().bucket(bucketName)
                                .build())
                .locationConstraintAsString();
    }

    @Override
    protected void connectToRepository(Repository repository, AuthenticationInfo authenticationInfo,
                                       ProxyInfoProvider proxyInfoProvider) throws AuthenticationException {
        if (this.amazonS3 == null) {
            AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
            this.bucketName = S3Utils.getBucketName(repository);
            this.baseDirectory = S3Utils.getBaseDirectory(repository);
            this.amazonS3 = S3Client.builder()
                    .credentialsProvider(credentialsProvider)
                    // TODO:
                    .region(Region.AP_NORTHEAST_1)
                    .build();
        }
    }

    @Override
    protected void disconnectFromRepository() {
        this.amazonS3 = null;
        this.bucketName = null;
        this.baseDirectory = null;
    }

    @Override
    protected boolean doesRemoteResourceExist(String resourceName) {
        try {
            getObjectMetadata(this.amazonS3, this.bucketName, this.baseDirectory, resourceName);
            return true;
        } catch (AwsServiceException e) {
            return false;
        }
    }

    @Override
    protected boolean isRemoteResourceNewer(String resourceName, long timestamp) throws ResourceDoesNotExistException {
        try {
            Instant lastModified = getObjectMetadata(this.amazonS3, this.bucketName, this.baseDirectory, resourceName).lastModified();
            return lastModified == null || Date.from(lastModified).getTime() > timestamp;
        } catch (AwsServiceException e) {
            throw new ResourceDoesNotExistException(String.format("'%s' does not exist", resourceName), e);
        }
    }

    @Override
    protected List<String> listDirectory(String directory) throws ResourceDoesNotExistException {
        List<String> directoryContents = new ArrayList<>();

        try {
            String prefix = getKey(this.baseDirectory, directory);
            Pattern pattern = Pattern.compile(String.format(RESOURCE_FORMAT, prefix));
            ListObjectsResponse objectListing = this.amazonS3.listObjects(
                    ListObjectsRequest.builder().bucket(bucketName)
                            .prefix(prefix).delimiter("/").build());
            directoryContents.addAll(getResourceNames(objectListing, pattern));

            if (objectListing.isTruncated() != null) {
                while (objectListing.isTruncated()) {
                    objectListing = this.amazonS3.listObjects(
                            ListObjectsRequest.builder().bucket(bucketName)
                                    .prefix(prefix).delimiter("/").build());
                    directoryContents
                            .addAll(getResourceNames(objectListing, pattern));
                }
            }

            if (CollectionUtils.isNullOrEmpty(directoryContents)) {
                throw AwsServiceException.builder()
                        .message(directory + " not found.").build();
            }
            return directoryContents;
        } catch (AwsServiceException e) {
            throw new ResourceDoesNotExistException(String.format("'%s' does not exist", directory), e);
        }
    }

    @Override
    protected void getResource(String resourceName, File destination, TransferProgress transferProgress)
            throws TransferFailedException, ResourceDoesNotExistException {
        try (ResponseInputStream<GetObjectResponse> s3Object = this.amazonS3
                .getObject(GetObjectRequest.builder().bucket(bucketName)
                        .key(getKey(this.baseDirectory, resourceName)).build());
             OutputStream out = new TransferProgressFileOutputStream(destination, transferProgress)) {
            IOUtils.copy(s3Object, out);
        } catch (AwsServiceException e) {
            throw new ResourceDoesNotExistException(String.format("'%s' does not exist", resourceName), e);
        } catch (FileNotFoundException e) {
            throw new TransferFailedException(String.format("Cannot write file to '%s'", destination), e);
        } catch (IOException e) {
            throw new TransferFailedException(String.format("Cannot read from '%s' and write to '%s'", resourceName, destination), e);
        }
    }

    @Override
    protected void putResource(File source, String destination, TransferProgress transferProgress) throws TransferFailedException,
            ResourceDoesNotExistException {
        String key = getKey(this.baseDirectory, destination);

        mkdirs(amazonS3, this.bucketName, key, 0);

        try (InputStream in = new TransferProgressFileInputStream(source, transferProgress)) {
            this.amazonS3.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key(key)
                            .build(),
                    RequestBody.fromInputStream(in, source.length()));
        } catch (AwsServiceException e) {
            throw new TransferFailedException(String.format("Cannot write file to '%s'", destination), e);
        } catch (FileNotFoundException e) {
            throw new ResourceDoesNotExistException(String.format("Cannot read file from '%s'", source), e);
        } catch (IOException e) {
            throw new TransferFailedException(String.format("Cannot write file to '%s'", destination), e);
        }
    }
}
