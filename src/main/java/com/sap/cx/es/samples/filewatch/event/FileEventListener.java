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
package com.sap.cx.es.samples.filewatch.event;

import java.util.EventListener;

/**
 *
 *
 * @author Alexei Liubimov <alexei.liubimov@sap.com>
 * @package com.sap.cx.es.samples.filewatch.event
 * @link http://sap.com/
 * @copyright 2020 SAP
 */
public interface FileEventListener extends EventListener
{
	void onCreated(FileEvent event);
	void onModified(FileEvent event);
	void onDeleted(FileEvent event);
}
