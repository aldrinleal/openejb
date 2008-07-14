/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.superbiz.registry;

import static javax.ejb.LockType.READ;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ComponentRegistryBean implements ComponentRegistry {

    private final Map<Class, Object> components = new HashMap<Class, Object>();

    @Lock(READ)
    public <T> T getComponent(Class<T> type) {
        return (T) components.get(type);
    }

    public <T> T setComponent(Class<T> type, T value) {
        return (T) components.put(type, value);
    }

    public <T> T removeComponent(Class<T> type) {
        return (T) components.remove(type);
    }

}