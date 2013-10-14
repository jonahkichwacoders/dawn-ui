/*-
 * Copyright 2013 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dawb.workbench.ui.views;

/**
 * Adaptor class to be overridden. This allows tasks to be defined to run in repetition and
 * also once the repetition is over
 */
public class SlowFastRunnable implements Runnable {
	private boolean fast = false;

	final public void setFast(boolean fast) {
		this.fast = fast;
	}

	final public boolean isFast() {
		return fast;
	}

	/**
	 * Override this to repeatedly perform a task
	 */
	@Override
	public void run() {
	}

	/**
	 * Override this to perform a task once repetition is over
	 */
	public void stop() {
	}
}