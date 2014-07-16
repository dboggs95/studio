/*
 * Copyright (c) 2013-2014, Neuro4j.org
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
package org.neuro4j.studio.core.relation.impl;

import org.neuro4j.workflow.node.WorkflowNode;
import org.neuro4j.studio.core.ActionNode;
import org.neuro4j.studio.core.OperatorOutput;
import org.neuro4j.studio.core.XmlTransition;
import org.neuro4j.studio.core.impl.ActionNodeImpl;
import org.neuro4j.studio.core.relation.ActionRelationProcessor;

public class FollowByRelationsNodeRelationProcessor extends ActionRelationProcessor {

    @Override
    protected void processOutpuNode(ActionNodeImpl source, WorkflowNode entity, OperatorOutput output, XmlTransition relation) {
        output.setName(relation.getName());
        processOutpuNode(source, null, output);
    }

    @Override
    public boolean processOutpuNode(ActionNode source, ActionNode target,
            OperatorOutput output) {
        source.getOutput().add(output);
        return true;

    }

    @Override
    public boolean updateOutpuNode(ActionNode source, ActionNode target,
            OperatorOutput output, String oldValue, String newValue) {
        return false;
    }

    @Override
    public boolean processOutpuNode(ActionNode source, String sourceAnchor,
            ActionNode target, String targetAnchor, OperatorOutput output) {
        source.getOutput().add(output);
        return true;
    }

}
