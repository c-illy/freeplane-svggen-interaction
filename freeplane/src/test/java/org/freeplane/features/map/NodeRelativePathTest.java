/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2014 Dimitry
 *
 *  This file author is Dimitry
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Dimitry Polivaev
 * 22.03.2014
 */
public class NodeRelativePathTest {
	MapModel map = null;

	private NodeModel root() {
	    final NodeModel parent = new NodeModel("parent", map);
		parent.setClones(new SingleNodeList(parent));
	    return parent;
    }

	@Test
	public void zeroLevelAncestor(){
		final NodeModel parent = root();
		NodeModel commonAncestor = new NodeRelativePath(parent, parent).commonAncestor();
		assertThat(commonAncestor, equalTo(parent));
	}

	@Test
	public void zeroLevelBegin(){
		final NodeModel parent = root();
		final NodeRelativePath nodeRelativePath = new NodeRelativePath(parent, parent);
		final NodeModel startingPoint = new NodeModel("startingPoint", map);
		assertThat(nodeRelativePath.pathBegin(startingPoint), equalTo(startingPoint));
	}

	@Test
	public void childLevelBegin(){
		final NodeModel parent = root();
		final NodeModel node = new NodeModel("node", map);
		parent.insert(node);
		final NodeRelativePath nodeRelativePath = new NodeRelativePath(node, parent);
		final NodeModel startingPoint = new NodeModel("startingPoint", map);
		final NodeModel child = new NodeModel("child", map);
		startingPoint.insert(child);
		assertThat(nodeRelativePath.pathBegin(startingPoint), equalTo(child));
	}
	@Test
	public void zeroLevelEnd(){
		final NodeModel parent = root();
		final NodeRelativePath nodeRelativePath = new NodeRelativePath(parent, parent);
		final NodeModel startingPoint = new NodeModel("startingPoint", map);
		assertThat(nodeRelativePath.pathEnd(startingPoint), equalTo(startingPoint));
	}

	@Test
	public void oneLevelAncestor(){
		final NodeModel parent = root();
		final NodeModel node1 = new NodeModel("node1", map);
		parent.insert(node1);
		final NodeModel node2 = new NodeModel("node2", map);
		parent.insert(node2);
		NodeModel commonAncestor = new NodeRelativePath(node1, node2).commonAncestor();
		assertThat(commonAncestor, equalTo(parent));
	}
	
	@Test
	public void equalPaths(){
		final NodeModel parent = root();
		final NodeModel node1 = new NodeModel("node1", map);
		parent.insert(node1);
		final NodeModel node2 = new NodeModel("node2", map);
		parent.insert(node2);
		final NodeRelativePath nodeRelativePath1 = new NodeRelativePath(node1, node2);
		final NodeRelativePath nodeRelativePath2 = new NodeRelativePath(node1, node2);
		assertTrue(nodeRelativePath1.equalPathsTo(nodeRelativePath2));
	}
}
