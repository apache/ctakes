/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.core.fsm.condition;

import org.apache.ctakes.core.fsm.token.WordToken;

import net.openai.util.fsm.Condition;

/**
 * Handles case where 2:00 is a single WordToken.
 * 
 * @author Mayo Clinic
 */
@SuppressWarnings("serial")
public class HourMinuteCondition extends Condition {
	private int iv_minMinute;
	private int iv_maxMinute;
	private int iv_minHour;
	private int iv_maxHour;

	public HourMinuteCondition(int minHour, int maxHour, int minMinute,
			int maxMinute) {
		iv_minMinute = minMinute;
		iv_maxMinute = maxMinute;
		iv_minHour = minHour;
		iv_maxHour = maxHour;
	}

	public boolean satisfiedBy(Object conditional) {
		if (conditional instanceof WordToken) {
			WordToken wt = (WordToken) conditional;
			String text = wt.getText();
			if (wt.getNumPosition() == WordToken.NUM_FIRST) {
				int colonIndex = text.indexOf(':');
				if (colonIndex != -1) {
					String hourStr = text.substring(0, colonIndex);
					String minuteStr = text.substring(colonIndex + 1, text
							.length());
					try {
						int hour = Integer.parseInt(hourStr);
						int minutes = Integer.parseInt(minuteStr);
						if ((hour >= iv_minHour) && (hour <= iv_maxHour)
								&& (minutes >= iv_minMinute)
								&& (minutes <= iv_maxMinute)) {
							return true;
						}
					} catch (NumberFormatException nfe) {
						return false;
					}
				}
			}
		}
		return false;
	}

}