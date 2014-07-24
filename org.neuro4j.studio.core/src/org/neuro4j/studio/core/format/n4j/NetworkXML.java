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
package org.neuro4j.studio.core.format.n4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.neuro4j.studio.core.format.f4j.FlowXML;
import org.neuro4j.studio.core.format.f4j.NodeXML;
import org.neuro4j.studio.core.format.f4j.TransitionXML;

@XmlRootElement(name = "network")
public class NetworkXML {

    @XmlJavaTypeAdapter(EntityListXMLAdapter.class)
    @XmlElement(name = "entities")
    List<EntityXML> entities = new ArrayList<EntityXML>();

    Map<String, EntityXML> map = null;

    public NetworkXML()
    {

    }

    public NetworkXML(FlowXML n)
    {
        for (NodeXML e : n.getXmlNodes())
        {
            entities.add(new EntityXML(e));

            for (TransitionXML transition : e.getRelations())
            {
                entities.add(new EntityXML(transition));
            }
        }

    }

    public List<EntityXML> getEntities() {
        return entities;
    }

    public EntityXML getById(String uuid)
    {
        if (map == null)
        {
            map = new HashMap<String, EntityXML>();
            for (EntityXML e : entities)
            {
                map.put(e.getUuid(), e);
            }
        }

        return map.get(uuid);
    }

}
