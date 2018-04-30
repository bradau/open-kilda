/*
 * Copyright 2017 Telstra Open Source
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

package org.openkilda.floodlight.operation.flow;

import org.openkilda.floodlight.exc.InsufficientCapabilitiesException;
import org.openkilda.floodlight.operation.Operation;
import org.openkilda.floodlight.operation.OperationContext;
import org.openkilda.messaging.command.flow.UniFlowVerificationRequest;
import org.openkilda.messaging.info.flow.FlowVerificationErrorCode;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VerificationDispatchOperation extends AbstractVerificationOperation {
    private static final Logger log = LoggerFactory.getLogger(VerificationDispatchOperation.class);

    private final IOFSwitchService switchService;

    public VerificationDispatchOperation(OperationContext context, UniFlowVerificationRequest verificationRequest) {
        super(context, verificationRequest);

        FloodlightModuleContext moduleContext = getContext().getModuleContext();
        switchService = moduleContext.getServiceImpl(IOFSwitchService.class);
    }

    @Override
    public void run() {
        launchSubOperations(produceSubOperations());
    }

    List<Optional<Operation>> produceSubOperations() {
        List<Optional<Operation>> plan = new ArrayList<>();

        try {
            // TODO(surabujin): will need to synchronise send/receive readiness in multi FL environment
            plan.add(makeReceiveOperation());
            plan.add(makeSendOperation());
        } catch (InsufficientCapabilitiesException e) {
            log.error(
                    "Unable to perform flow VERIFICATION due to {} (packetID: {})",
                    e.toString(), getVerificationRequest().getPacketId());
            sendErrorResponse(FlowVerificationErrorCode.NOT_CAPABLE);
            plan.clear();
        }

        return plan;
    }

    private void launchSubOperations(List<Optional<Operation>> subOperations) {
        for (Optional<Operation> operation : subOperations) {
            operation.ifPresent(this::startSubOperation);
        }
    }

    private Optional<Operation> makeSendOperation() {
        UniFlowVerificationRequest verificationRequest = getVerificationRequest();
        if (!isOwnSwitch(verificationRequest.getSourceSwitchId())) {
            log.debug("Switch {} is not under our control, do not produce flow verification send request");
            return Optional.empty();
        }

        log.debug("Initiate verification send operation (request: {})", verificationRequest.getPacketId());
        return Optional.of(new VerificationSendOperation(getContext(), verificationRequest));
    }

    private Optional<Operation> makeReceiveOperation() throws InsufficientCapabilitiesException {
        UniFlowVerificationRequest verificationRequest = getVerificationRequest();
        if (!isOwnSwitch(verificationRequest.getDestSwitchId())) {
            log.debug("Switch {} is not under our control, do not produce flow verification receive handler");
            return Optional.empty();
        }

        log.debug("Initiate verification listen operation (request: {})", verificationRequest.getPacketId());
        return Optional.of(new VerificationListenOperation(getContext(), verificationRequest));
    }

    private boolean isOwnSwitch(String switchId) {
        DatapathId dpId = DatapathId.of(switchId);
        IOFSwitch sw = switchService.getActiveSwitch(dpId);

        return sw != null;
    }
}
