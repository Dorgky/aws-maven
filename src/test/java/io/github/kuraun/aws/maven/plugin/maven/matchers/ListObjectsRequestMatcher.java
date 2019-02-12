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

package io.github.kuraun.aws.maven.plugin.maven.matchers;

import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;

public class ListObjectsRequestMatcher implements ArgumentMatcher<ListObjectsRequest> {

    private final ListObjectsRequest listObjectsRequest;

    ListObjectsRequestMatcher(ListObjectsRequest listObjectsRequest) {
        this.listObjectsRequest = listObjectsRequest;
    }

    @Override
    public boolean matches(ListObjectsRequest obj) {
        if (this.listObjectsRequest == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (ListObjectsRequest.class != obj.getClass()) {
            return false;
        }
        ListObjectsRequest other = obj;
        if (this.listObjectsRequest.bucket() == null) {
            if (other.bucket() != null) {
                return false;
            }
        } else if (!this.listObjectsRequest.bucket().equals(other.bucket())) {
            return false;
        }
        if (this.listObjectsRequest.prefix() == null) {
            if (other.prefix() != null) {
                return false;
            }
        } else if (!this.listObjectsRequest.prefix().equals(other.prefix())) {
            return false;
        }
        if (this.listObjectsRequest.delimiter() == null) {
            if (other.delimiter() != null) {
                return false;
            }
        } else if (!this.listObjectsRequest.delimiter().equals(other.delimiter())) {
            return false;
        }
        if (this.listObjectsRequest.marker() == null) {
            if (other.marker() != null) {
                return false;
            }
        } else if (!this.listObjectsRequest.marker().equals(other.marker())) {
            return false;
        }
        if (this.listObjectsRequest.maxKeys() == null) {
            if (other.maxKeys() != null) {
                return false;
            }
        } else if (!this.listObjectsRequest.maxKeys().equals(other.maxKeys())) {
            return false;
        }
        return true;
    }
}