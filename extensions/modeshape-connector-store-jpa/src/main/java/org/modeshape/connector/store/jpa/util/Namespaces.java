/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.connector.store.jpa.util;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.connector.store.jpa.model.common.NamespaceEntity;

/**
 * A utility that wraps the {@link NamespaceEntity} objects appearing within an {@link EntityManager}.
 */
@NotThreadSafe
public class Namespaces {

    private final EntityManager entityManager;
    private final Map<String, NamespaceEntity> cache = new HashMap<String, NamespaceEntity>();

    public Namespaces( EntityManager manager ) {
        this.entityManager = manager;
    }

    public NamespaceEntity get( String namespaceUri,
                                boolean createIfRequired ) {
        NamespaceEntity entity = cache.get(namespaceUri);
        if (entity == null) {
            entity = NamespaceEntity.findByUri(entityManager, namespaceUri, createIfRequired);
            if (entity != null) {
                cache.put(namespaceUri, entity);
            }
        }
        return entity;
    }
}
