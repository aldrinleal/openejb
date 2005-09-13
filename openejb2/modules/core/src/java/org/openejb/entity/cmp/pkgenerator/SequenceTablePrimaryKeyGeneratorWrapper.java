/* ====================================================================
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce this list of
 *    conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The name "OpenEJB" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of The OpenEJB Group.  For written permission,
 *    please contact openejb-group@openejb.sf.net.
 *
 * 4. Products derived from this Software may not be called "OpenEJB"
 *    nor may "OpenEJB" appear in their names without prior written
 *    permission of The OpenEJB Group. OpenEJB is a registered
 *    trademark of The OpenEJB Group.
 *
 * 5. Due credit should be given to the OpenEJB Project
 *    (http://openejb.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE OPENEJB GROUP AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * THE OPENEJB GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the OpenEJB Project.  For more information
 * please see <http://openejb.org/>.
 *
 * ====================================================================
 */
package org.openejb.entity.cmp.pkgenerator;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.ManagedConnectionFactoryWrapper;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.transaction.context.TransactionContextManager;
import org.tranql.cache.CacheRow;
import org.tranql.cache.DuplicateIdentityException;
import org.tranql.cache.InTxCache;
import org.tranql.identity.GlobalIdentity;
import org.tranql.pkgenerator.PrimaryKeyGenerator;
import org.tranql.pkgenerator.PrimaryKeyGeneratorException;
import org.tranql.pkgenerator.SequenceTablePrimaryKeyGenerator;

/**
 *
 *
 * @version $Revision$ $Date$
 */
public class SequenceTablePrimaryKeyGeneratorWrapper implements PrimaryKeyGenerator, GBeanLifecycle {
    private static final Log log = LogFactory.getLog(SequenceTablePrimaryKeyGeneratorWrapper.class);

    private final TransactionContextManager transactionContextManager;
    private final ManagedConnectionFactoryWrapper connectionFactoryWrapper;
    private final String tableName;
    private final String sequenceName;
    private final int batchSize;
    private SequenceTablePrimaryKeyGenerator delegate; 

    public SequenceTablePrimaryKeyGeneratorWrapper(TransactionContextManager transactionContextManager, ManagedConnectionFactoryWrapper connectionFactoryWrapper, String tableName, String sequenceName, int batchSize) {
        this.transactionContextManager = transactionContextManager;
        this.connectionFactoryWrapper = connectionFactoryWrapper;
        this.tableName = tableName;
        this.sequenceName = sequenceName;
        this.batchSize = batchSize;
    }
    
    public void doStart() throws Exception {
        DataSource dataSource = (DataSource) connectionFactoryWrapper.$getResource();
        TransactionManager tm = transactionContextManager.getTransactionManager();
        delegate = new SequenceTablePrimaryKeyGenerator(tm, dataSource, tableName, sequenceName, batchSize);
        delegate.initSequenceTable();
    }

    public void doStop() throws Exception {
        delegate = null;
    }

    public void doFail() {
        delegate = null;
    }

    public Object getNextPrimaryKey(CacheRow cacheRow) throws PrimaryKeyGeneratorException {
        return delegate.getNextPrimaryKey(cacheRow);
    }

    public CacheRow updateCache(InTxCache cache, GlobalIdentity id, CacheRow cacheRow) throws DuplicateIdentityException {
        return delegate.updateCache(cache, id, cacheRow);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = new GBeanInfoBuilder(SequenceTablePrimaryKeyGeneratorWrapper.class);
        infoFactory.addInterface(PrimaryKeyGenerator.class);
        
        infoFactory.addReference("TransactionContextManager", TransactionContextManager.class, NameFactory.JTA_RESOURCE);
        infoFactory.addReference("ManagedConnectionFactoryWrapper", ManagedConnectionFactoryWrapper.class, NameFactory.JCA_MANAGED_CONNECTION_FACTORY);
        infoFactory.addAttribute("tableName", String.class, true);
        infoFactory.addAttribute("sequenceName", String.class, true);
        infoFactory.addAttribute("batchSize", int.class, true);

        infoFactory.setConstructor(new String[]{"TransactionContextManager", "ManagedConnectionFactoryWrapper", "tableName", "sequenceName", "batchSize"});

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
