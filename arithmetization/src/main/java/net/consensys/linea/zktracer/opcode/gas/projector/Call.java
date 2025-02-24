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

import static net.consensys.linea.zktracer.types.AddressUtils.isAddressWarm;

import lombok.RequiredArgsConstructor;
import net.consensys.linea.zktracer.module.constants.GlobalConstants;
import net.consensys.linea.zktracer.types.Range;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.internal.Words;

@RequiredArgsConstructor
public class Call extends GasProjection {
  private final MessageFrame frame;
  private final long stipend;
  private final Range inputData;
  private final Range returnData;
  private final Wei value;
  private final Account recipient;
  private final Address to;

  public static Call invalid() {
    return new Call(null, 0, Range.empty(), Range.empty(), Wei.ZERO, null, null);
  }

  boolean isInvalid() {
    return this.frame == null;
  }

  @Override
  public long memoryExpansion() {
    if (this.isInvalid()) {
      return 0;
    }

    return Math.max(
        gc.memoryExpansionGasCost(frame, inputData.offset(), inputData.size()),
        gc.memoryExpansionGasCost(frame, returnData.offset(), returnData.size()));
  }

  @Override
  public long largestOffset() {
    if (this.isInvalid()) {
      return 0;
    }

    return Math.max(
        inputData.isEmpty() ? 0 : Words.clampedAdd(inputData.offset(), inputData.size()),
        returnData.isEmpty() ? 0 : Words.clampedAdd(returnData.offset(), returnData.size()));
  }

  @Override
  public long accountAccess() {
    if (this.isInvalid()) {
      return 0;
    }

    if (isAddressWarm(frame, to)) {
      return GlobalConstants.GAS_CONST_G_WARM_ACCESS;
    } else {
      return GlobalConstants.GAS_CONST_G_COLD_ACCOUNT_ACCESS;
    }
  }

  @Override
  public long accountCreation() {
    if (this.isInvalid()) {
      return 0;
    }

    if ((recipient == null || recipient.isEmpty()) && !value.isZero()) {
      return GlobalConstants.GAS_CONST_G_NEW_ACCOUNT;
    } else {
      return 0L;
    }
  }

  @Override
  public long transferValue() {
    if (this.isInvalid()) {
      return 0;
    }

    if (value.isZero()) {
      return 0L;
    } else {
      return GlobalConstants.GAS_CONST_G_CALL_VALUE;
    }
  }

  @Override
  public long gasPaidOutOfPocket() {
    if (this.isInvalid()) {
      return 0;
    }

    final long upfrontGasCost =
        memoryExpansion() + accountAccess() + accountCreation() + transferValue();
    if (upfrontGasCost > frame.getRemainingGas()) {
      return 0L;
    }

    final long remaining = frame.getRemainingGas() - upfrontGasCost;
    final long sixtyThreeSixtyFourthsOfRemaining = remaining - remaining / 64;
    return Math.min(sixtyThreeSixtyFourthsOfRemaining, stipend);
  }

  @Override
  public long stipend() {
    if (this.isInvalid() || this.value.isZero()) {
      return 0;
    }

    return GlobalConstants.GAS_CONST_G_CALL_STIPEND;
  }
}
