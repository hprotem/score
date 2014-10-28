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
package com.hp.score.orchestrator.entities;

import com.hp.score.facade.entities.Execution;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

/**
 * Created by IntelliJ IDEA.
 * User: Amit Levin
 * Date: 28/11/13
 */
@Embeddable
public class ExecutionObjEntity {


    @Lob
    @Column(name = "EXECUTION_OBJECT")
    private Execution executionObj;


    public Execution getExecutionObj() {
        return executionObj;
    }

    public void setExecutionObj(Execution executionObj) {
        this.executionObj = executionObj;
    }

    public ExecutionObjEntity(){

    }

    public ExecutionObjEntity(Execution executionObj){
           this.executionObj = executionObj;
    }



}
