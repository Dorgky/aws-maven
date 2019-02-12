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

package io.github.kuraun.aws.maven.plugin.data.transfer;

import io.github.kuraun.aws.maven.plugin.data.TransferListenerSupport;
import io.github.kuraun.aws.maven.plugin.data.TransferProgress;
import org.apache.maven.wagon.resource.Resource;

public final class StandardTransferProgress implements TransferProgress {

    private final Resource resource;

    private final int requestType;

    private final TransferListenerSupport transferListenerSupport;

    public StandardTransferProgress(Resource resource, int requestType, TransferListenerSupport transferListenerSupport) {
        this.resource = resource;
        this.requestType = requestType;
        this.transferListenerSupport = transferListenerSupport;
    }

    @Override
    public void notify(byte[] buffer, int length) {
        this.transferListenerSupport.fireTransferProgress(this.resource, this.requestType, buffer, length);
    }

}
