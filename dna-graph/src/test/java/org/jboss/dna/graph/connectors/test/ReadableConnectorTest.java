/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connectors.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.UUID;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.requests.CacheableRequest;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.jboss.dna.graph.requests.ReadAllPropertiesRequest;
import org.jboss.dna.graph.requests.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.requests.ReadBranchRequest;
import org.jboss.dna.graph.requests.ReadNextBlockOfChildrenRequest;
import org.jboss.dna.graph.requests.ReadNodeRequest;
import org.jboss.dna.graph.requests.ReadPropertyRequest;
import org.junit.After;
import org.junit.Test;

/**
 * A class that provides standard reading verification tests for connectors. This class is designed to be extended for each
 * connector, and in each subclass the {@link #setUpSource()} method is defined to provide a valid {@link RepositorySource} for
 * the connector to be tested.
 * <p>
 * Since these tests only use methods that never modify repository content, the repository is set up only once (before the first
 * test) and is shut down after all tests have completed.
 * </p>
 * 
 * @author Randall Hauch
 */
public abstract class ReadableConnectorTest extends AbstractConnectorTest {

    /**
     * Method that is executed after each test. This method does nothing, since the repository is set up once for all of the tests
     * and then shutdown after all tests have completed.
     * 
     * @throws Exception
     */
    @Override
    @After
    public void afterEach() throws Exception {
        // Don't shut down the repository
    }

    /**
     * Read the node at the supplied location, using a variety of techniques to read the node and compare that each technique
     * returned the same node. This method reads the entire node (via {@link Graph#getNodeAt(Location)}, which uses
     * {@link ReadNodeRequest}), reads all of the properties on the node (via {@link Graph#getProperties()}, which uses
     * {@link ReadAllPropertiesRequest}), and reads all of the children of the node (via {@link Graph#getChildren()}, which uses
     * {@link ReadAllChildrenRequest}).
     * 
     * @param location the location; may not be null
     * @return the node that was read
     */
    public Node readNodeThoroughly( Location location ) {
        assertThat(location, is(notNullValue()));
        Node result = null;
        if (location.hasPath() && location.hasIdProperties()) {
            // Read the node by the path ...
            Node resultByPath = graph.getNodeAt(new Location(location.getPath()));

            // Read the node by identification properties ...
            Node resultByIdProps = graph.getNodeAt(new Location(location.getIdProperties()));

            // Read the node by using the location with both a path and ID properties ...
            result = graph.getNodeAt(location);

            // Verify that the same node was returned by each ...
            assertSameNode(resultByPath, result);
            assertSameNode(resultByIdProps, result);

            // Check the result has the correct location ...
            assertThat("The node that was read doesn't have the expected location", result.getLocation(), is(location));
        } else {
            // Read the node by using the location (as is)
            result = graph.getNodeAt(location);

            // Check the result has the correct location ...
            assertThat("The node that was read doesn't have the expected location",
                       result.getLocation().isSame(location, true),
                       is(true));
        }

        // Read all the properties of the node ...
        assertSameProperties(result, graph.getProperties().on(location));

        // Read all the children of the node ...
        assertThat(graph.getChildren().of(location), is(result.getChildren()));

        return result;
    }

    @Test
    public void shouldAlwaysBeAbleToReadRootNode() {
        Node root = graph.getNodeAt("/");
        assertThat("Connector must always have a root node", root, is(notNullValue()));
        assertThat("Root node must always have a path", root.getLocation().hasPath(), is(true));
        assertThat("Root node must never have a null path", root.getLocation().getPath(), is(notNullValue()));
        assertThat("Root node's path must be the root path", root.getLocation().getPath().isRoot(), is(true));
        List<Property> idProperties = root.getLocation().getIdProperties();
        if (idProperties == null) {
            assertThat(root.getLocation().hasIdProperties(), is(false));
        } else {
            assertThat(root.getLocation().hasIdProperties(), is(true));
        }
    }

    @Test
    public void shouldReturnEquivalentLocationForRootUponRepeatedCalls() {
        Node root = graph.getNodeAt("/");
        for (int i = 0; i != 10; ++i) {
            Node anotherRoot = graph.getNodeAt("/");
            assertThat(anotherRoot.getLocation().equals(root.getLocation()), is(true));
            assertThat(anotherRoot.getLocation().getPath(), is(root.getLocation().getPath()));
            assertThat(anotherRoot.getLocation().getIdProperties(), is(root.getLocation().getIdProperties()));
        }
    }

    @Test
    public void shouldFindRootByIdentificationProperties() {
        Node root = graph.getNodeAt("/");
        Location rootLocation = root.getLocation();
        if (rootLocation.hasIdProperties()) {
            List<Property> idProperties = rootLocation.getIdProperties();
            assertThat("Root node's ID properties was null when there were supposed to be properties",
                       idProperties,
                       is(notNullValue()));
            Property firstProperty = idProperties.get(0);
            Property[] additionalProperties = new Property[] {};
            if (idProperties.size() > 1) {
                List<Property> morePropertiesList = idProperties.subList(1, idProperties.size());
                assertThat(morePropertiesList.isEmpty(), is(false));
                additionalProperties = morePropertiesList.toArray(new Property[morePropertiesList.size()]);
            }
            // Find the root node using the identification properties ...
            Node anotherRoot = graph.getNodeAt(firstProperty, additionalProperties);
            assertThat(anotherRoot.getLocation().equals(root.getLocation()), is(true));
            assertThat(anotherRoot.getLocation().getPath(), is(root.getLocation().getPath()));
            assertThat(anotherRoot.getLocation().getIdProperties(), is(root.getLocation().getIdProperties()));
        }
    }

    @Test
    public void shouldFindRootByUUID() {
        Node root = graph.getNodeAt("/");
        Location rootLocation = root.getLocation();
        UUID uuid = rootLocation.getUuid();
        if (uuid != null) {
            // Find the root node using the identification properties ...
            Node anotherRoot = graph.getNodeAt(uuid);
            assertThat(anotherRoot.getLocation().equals(root.getLocation()), is(true));
            assertThat(anotherRoot.getLocation().getPath(), is(root.getLocation().getPath()));
            assertThat(anotherRoot.getLocation().getIdProperties(), is(root.getLocation().getIdProperties()));
            assertThat(anotherRoot.getLocation().getUuid(), is(root.getLocation().getUuid()));
        }
    }

    @Test
    public void shouldReadTheChildrenOfTheRootNode() {
        List<Location> children = graph.getChildren().of("/");
        assertThat(children, is(notNullValue()));
        for (Location child : children) {
            // Check the location has a path that has the root as a parent ...
            assertThat(child.hasPath(), is(true));
            assertThat(child.getPath().getParent().isRoot(), is(true));

            // Verify that each node can be read multiple ways ...
            readNodeThoroughly(child);
        }
    }

    @Test
    public void shouldReadSubgraphStartingAtRootAndWithMaximumDepthOfThree() {
        Subgraph subgraph = graph.getSubgraphOfDepth(3).at("/");
        assertThat(subgraph, is(notNullValue()));

        // Verify that the root node is the same as getting it directly ...
        Node root = subgraph.getRoot();
        assertSameNode(root, graph.getNodeAt("/"));

        // Verify the first-level children ...
        List<Location> children = graph.getChildren().of("/");
        assertThat(children, is(notNullValue()));
        for (Location childLocation : children) {
            // Verify the child in the subgraph matches the same node obtained directly from the graph ...
            Node child = subgraph.getNode(childLocation);
            assertSameNode(child, graph.getNodeAt(childLocation));

            // Now get the second-level children ...
            List<Location> grandChildren = graph.getChildren().of(childLocation);
            assertThat(grandChildren, is(notNullValue()));
            for (Location grandchildLocation : grandChildren) {
                // Verify the grandchild in the subgraph matches the same node obtained directly from the graph ...
                Node grandchild = subgraph.getNode(grandchildLocation);
                assertSameNode(grandchild, graph.getNodeAt(grandchildLocation));

                // The subgraph should contain the children locations and properties for the grandchildren.
                // However, the subgraph should not a node for the children of the grandchildren ...
                for (Location greatGrandchild : grandchild.getChildren()) {
                    assertThat(subgraph.getNode(greatGrandchild), is(nullValue()));
                }
            }
        }
    }

    @Test
    public void shouldReadIndividualPropertiesOfNodes() {
        // Read each node that is a child of the root...
        for (Location childLocation : graph.getChildren().of("/")) {
            // For each node ...
            Node child = graph.getNodeAt(childLocation);
            for (Property property : child.getProperties()) {
                Name name = property.getName();
                // Re-read the property and verify the value(s) match the value(s) in 'property'
                Property singleProperty = graph.getProperty(name).on(childLocation);
                assertThat(singleProperty, is(notNullValue()));
                assertThat(singleProperty, is(property));
            }
        }
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReadNodeThatDoesNotExist() {
        // Look up the child that should not exist, and this should throw an exception ...
        Path nonExistantChildName = findPathToNonExistentNodeUnder("/");
        graph.getNodeAt(nonExistantChildName);
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReadPropertyOnNodeThatDoesNotExist() {
        // Look up the child that should not exist, and this should throw an exception ...
        Path nonExistantChildName = findPathToNonExistentNodeUnder("/");
        graph.getProperty("jcr:uuid").on(nonExistantChildName);
    }

    @Test
    public void shouldFailToReadPropertyThatDoesNotExistOnExistingNode() {
        // Read each node that is a child of the root...
        for (Location childLocation : graph.getChildren().of("/")) {
            // Find a name for a non-existing property ...
            String nonExistentPropertyName = "ab39dbyfg739_adf7bg";
            // For each node ...
            Property property = graph.getProperty(nonExistentPropertyName).on(childLocation); // will throw exception
            assertThat(property, is(nullValue()));
        }
    }

    @Test
    public void shouldIncludeTimeLoadedInReadNodeRequests() {
        CacheableRequest request = new ReadNodeRequest(location("/"));
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadAllPropertiesRequests() {
        CacheableRequest request = new ReadAllPropertiesRequest(location("/"));
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadAllChildrenRequests() {
        CacheableRequest request = new ReadAllChildrenRequest(location("/"));
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadBlockOfChildrenRequests() {
        CacheableRequest request = new ReadBlockOfChildrenRequest(location("/"), 0, 100);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadNextBlockOfChildrenRequests() {
        // Get the first child ...
        Location firstChild = graph.getChildren().of("/").get(0);
        CacheableRequest request = new ReadNextBlockOfChildrenRequest(firstChild, 100);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadPropertyRequests() {
        // Get each of the properties on the first child ...
        Location firstChildLocation = graph.getChildren().of("/").get(0);
        Node firstChild = graph.getNodeAt(firstChildLocation);
        for (Property property : firstChild.getProperties()) {
            CacheableRequest request = new ReadPropertyRequest(firstChildLocation, property.getName());
            execute(request);
            assertThat(request.getTimeLoaded(), is(notNullValue()));
        }
    }

    @Test
    public void shouldIncludeTimeLoadedInReadBranchRequests() {
        CacheableRequest request = new ReadBranchRequest(location("/"), 2);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

}