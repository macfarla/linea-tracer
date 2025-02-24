/*
 * Copyright Consensys Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package net.consensys.linea.zktracer.opcode.gas.projector;

import static org.hyperledger.besu.evm.internal.Words.clampedAdd;
import static org.hyperledger.besu.evm.internal.Words.clampedToLong;

import net.consensys.linea.zktracer.module.constants.GlobalConstants;
import org.hyperledger.besu.evm.frame.MessageFrame;

public final class Sha3 extends GasProjection {
  private final MessageFrame frame;
  private long offset = 0;
  private long length = 0;

  public Sha3(MessageFrame frame) {
    this.frame = frame;
    if (frame.stackSize() >= 2) {
      this.offset = clampedToLong(frame.getStackItem(0));
      this.length = clampedToLong(frame.getStackItem(1));
    }
  }

  @Override
  public long staticGas() {
    return GlobalConstants.GAS_CONST_G_KECCAK_256;
  }

  @Override
  public long memoryExpansion() {
    return gc.memoryExpansionGasCost(frame, offset, length);
  }

  @Override
  public long largestOffset() {
    return this.length == 0 ? 0 : clampedAdd(this.offset, this.length);
  }

  @Override
  public long linearPerWord() {
    return linearCost(GlobalConstants.GAS_CONST_G_KECCAK_256_WORD, this.length, 32);
  }

  @Override
  public long messageSize() {
    return this.length;
  }
}
