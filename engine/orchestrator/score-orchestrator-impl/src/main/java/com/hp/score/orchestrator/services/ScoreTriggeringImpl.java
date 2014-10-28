/*
 * Licensed to Hewlett-Packard Development Company, L.P. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package com.hp.score.orchestrator.services;

import com.hp.score.api.ExecutionPlan;
import com.hp.score.api.TriggeringProperties;
import com.hp.score.engine.data.IdentityGenerator;
import com.hp.score.engine.node.entities.WorkerNode;
import com.hp.score.engine.queue.entities.ExecStatus;
import com.hp.score.engine.queue.entities.ExecutionMessage;
import com.hp.score.engine.queue.entities.ExecutionMessageConverter;
import com.hp.score.engine.queue.entities.Payload;
import com.hp.score.engine.queue.services.QueueDispatcherService;
import com.hp.score.facade.entities.Execution;
import com.hp.score.facade.services.RunningExecutionPlanService;
import com.hp.score.lang.SystemContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * User: wahnonm
 * Date: 30/01/14
 * Time: 14:19
 */
public class ScoreTriggeringImpl implements ScoreTriggering {

    @Autowired
    private RunningExecutionPlanService runningExecutionPlanService;

    @Autowired
    private IdentityGenerator idGenerator;

    @Autowired
    private QueueDispatcherService queueDispatcher;

    @Autowired
    private ExecutionMessageConverter executionMessageConverter;

    @Autowired
    private ExecutionStateService executionStateService;

    @Override
    public Long trigger(TriggeringProperties triggeringProperties) {
        Long executionId = idGenerator.next();
        return trigger(executionId, triggeringProperties);
    }

    @Override
    public Long trigger(Long executionId, TriggeringProperties triggeringProperties) {
        SystemContext scoreSystemContext = new SystemContext(triggeringProperties.getRuntimeValues());
        Long runningExecutionPlanId = saveRunningExecutionPlans(triggeringProperties.getExecutionPlan(), triggeringProperties.getDependencies(), scoreSystemContext);
        scoreSystemContext.setExecutionId(executionId);
        Execution execution = new Execution(executionId, runningExecutionPlanId, triggeringProperties.getStartStep(), triggeringProperties.getContext(), scoreSystemContext);

        // create execution record in ExecutionSummary table
        executionStateService.createParentExecution(execution.getExecutionId());

        // create execution message
        ExecutionMessage message = createExecutionMessage(execution);
        enqueue(message);
        return executionId;
    }

    private Long saveRunningExecutionPlans(ExecutionPlan executionPlan, Map<String, ExecutionPlan> dependencies, SystemContext systemContext) {
        Map<String, Long> runningPlansIds = new HashMap<>();
        Map<String, Long> beginStepsIds = new HashMap<>();

        if(dependencies != null) {
            for (ExecutionPlan dependencyExecutionPlan : dependencies.values()) {
                String subFlowUuid = dependencyExecutionPlan.getFlowUuid();
                Long subFlowRunningId = runningExecutionPlanService.getOrCreateRunningExecutionPlan(dependencyExecutionPlan);
                runningPlansIds.put(subFlowUuid, subFlowRunningId);
                beginStepsIds.put(subFlowUuid, dependencyExecutionPlan.getBeginStep());
            }
        }

        // Adding the ids of the running execution plan of the parent + its begin step
        // since this map should contain all the ids of the running plans
        Long runningPlanId =  runningExecutionPlanService.getOrCreateRunningExecutionPlan(executionPlan);
        runningPlansIds.put(executionPlan.getFlowUuid(), runningPlanId);
        beginStepsIds.put(executionPlan.getFlowUuid(), executionPlan.getBeginStep());

        systemContext.setSubFlowsData(runningPlansIds, beginStepsIds);

        return runningPlanId;
    }

    private void enqueue(ExecutionMessage... messages) {
        queueDispatcher.dispatch(Arrays.asList(messages));
    }

    private ExecutionMessage createExecutionMessage(Execution execution) {
        Payload payload = executionMessageConverter.createPayload(execution);

        return new ExecutionMessage(ExecutionMessage.EMPTY_EXEC_STATE_ID,
                ExecutionMessage.EMPTY_WORKER,
                WorkerNode.DEFAULT_WORKER_GROUPS[0],
                String.valueOf(execution.getExecutionId()),
                ExecStatus.PENDING, //start new flow also in PENDING
                payload,
                0);
    }
}
