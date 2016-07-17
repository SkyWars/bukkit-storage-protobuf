/*
 * Copyright (C) 2016 Dabo Ross <http://www.daboross.net/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.daboross.bukkitdev.bukkitstorageprotobuf.operations;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public class MemoryBlockAreaCopyOperationTest {

    @Parameterized.Parameters(name = "{index}: (operationSize={0}, lengthX={1}, lengthY={2}, lengthZ={3}")
    public static Collection<Object[]> randomParameters() {
        Collection<Object[]> parameters = new ArrayList<Object[]>();
        for (int operationSize = 500; operationSize < 1000; operationSize += 92) {
            for (int lengthX = 1; lengthX < 200; lengthX += 61) {
                for (int lengthY = 1; lengthY < 200; lengthY += 86) {
                    for (int lengthZ = 1; lengthZ < 200; lengthZ += 77) {
                        parameters.add(new Object[]{operationSize, lengthX, lengthY, lengthZ});
                    }
                }
            }
        }
        return parameters;
    }

    @Parameter
    public int operationSize;

    @Parameter(value = 1)
    public int lengthX;

    @Parameter(value = 2)
    public int lengthY;

    @Parameter(value = 3)
    public int lengthZ;

    @Test
    public void performNextPart() {
        TestOperation operation = new TestOperation(operationSize, lengthX, lengthY, lengthZ);

        while (operation.getPartsLeft() > 0) {
            operation.performNextPart();
        }

        for (int x = 0; x < lengthX; x++) {
            for (int y = 0; y < lengthY; y++) {
                for (int z = 0; z < lengthZ; z++) {
                    if (!operation.blocksCopied[x][y][z]) {
                        // String formatting would be expensive if done for every block,
                        // so use Assert.fail() with a boolean check not Assert.assertTrue().
                        Assert.fail(String.format(" [%d][%d][%d]", x, y, z));
                    }
                }
            }
        }
    }

    private class TestOperation extends MemoryBlockAreaCopyOperation {

        private boolean[][][] blocksCopied;

        public TestOperation(int operationSize, int lengthX, int lengthY, int lengthZ) {
            super(null, operationSize, null, -20, -20, -20, null, lengthX, lengthY, lengthZ);
            blocksCopied = new boolean[lengthX][lengthY][lengthZ];
        }

        @Override
        protected void copyBlock(final int x, final int y, final int z) {
            if (blocksCopied[x][y][z]) {
                Assert.fail(String.format("Double copied block at [%d][%d][%d]", x, y, z));
            }
            blocksCopied[x][y][z] = true;
        }
    }
}
