/**
 * ***********************************************************************
 * Copyright (c) 2020, SAP <sap.com>
 * <p>
 * All portions of the code written by SAP are property of SAP.
 * All Rights Reserved.
 * <p>
 * SAP
 * <p>
 * Moscow, Russian Federation
 * <p>
 * Web: sap.com
 * ***********************************************************************
 */
package com.sap.cx.es.samples.filewatch;

import com.sap.cx.es.samples.filewatch.event.FileEvent;
import com.sap.cx.es.samples.filewatch.event.FileEventListener;

/**
 *
 *
 * @author Alexei Liubimov <alexei.liubimov@sap.com>
 * @package com.sap.cx.es.samples.filewatch
 * @link http://sap.com/
 * @copyright 2020 SAP
 */
public abstract class FileAdapter implements FileEventListener {

	@Override
	public void onCreated(FileEvent event) {
		// do nothing
	}

	@Override
	public void onModified(FileEvent event) {
		// do nothing
	}

	@Override
	public void onDeleted(FileEvent event) {
		// do nothing
	}
}
