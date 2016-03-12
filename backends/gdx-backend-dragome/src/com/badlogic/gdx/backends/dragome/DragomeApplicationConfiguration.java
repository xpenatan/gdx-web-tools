/*******************************************************************************
 * Copyright 2016 Natan Guilherme.
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
 ******************************************************************************/

package com.badlogic.gdx.backends.dragome;

/** @author xpenatan */
public class DragomeApplicationConfiguration {
	/** whether to use a stencil buffer **/
	public boolean stencil = false;
	public boolean antialiasing = false;
	public boolean alpha = false;
	public boolean premultipliedAlpha = false;
	public boolean preserveDrawingBuffer = false;
	public boolean useDebugGL = false;
}
