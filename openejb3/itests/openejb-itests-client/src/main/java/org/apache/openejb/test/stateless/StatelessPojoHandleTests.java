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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.test.stateless;

import javax.ejb.EJBObject;

/**
 * @author <a href="mailto:david.blevins@visi.com">David Blevins</a>
 * @author <a href="mailto:Richard@Monson-Haefel.com">Richard Monson-Haefel</a>
 * @author <a href="mailto:manu.t.george@gmail.com">Manu George</a>
 */
public class StatelessPojoHandleTests extends BasicStatelessTestClient {

    public StatelessPojoHandleTests() {
        super("PojoHandle.");
    }

    protected void setUp() throws Exception {
        super.setUp();
        Object obj = initialContext.lookup("client/tests/stateless/BasicStatelessPojoHome");
        ejbHome = (BasicStatelessHome) javax.rmi.PortableRemoteObject.narrow(obj, BasicStatelessHome.class);
        ejbObject = ejbHome.createObject();
        ejbHandle = ejbObject.getHandle();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    //=================================
    // Test handle methods
    //
    public void test01_getEJBObject() {

        try {
            EJBObject object = ejbHandle.getEJBObject();
            assertNotNull("The EJBObject is null", object);
            assertTrue("EJBObjects are not identical", object.isIdentical(ejbObject));
        } catch (Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }

    /**
     * <B>3.6.6 Client view of session object's life cycle</B>
     * <p/>
     * ....It is invalid to reference a session object that does
     * not exist. Attempted invocations on a session object
     * that does not exist result in java.rmi.NoSuchObjectException.
     * </P>
     * <p/>
     * <p/>
     * This remove method of the EJBHome is placed hear as it
     * is more a test on the handle then on the remove method
     * itself.
     * </P>
     */
    public void test02_EJBHome_remove() {
        try {
            ejbHome.remove(ejbHandle);
            // you can't really remove a stateless handle
            ejbObject.businessMethod("Should not throw an exception");
        } catch (Exception e) {
            fail("Received Exception " + e.getClass() + " : " + e.getMessage());
        }
    }
    //
    // Test handle methods
    //=================================

}
