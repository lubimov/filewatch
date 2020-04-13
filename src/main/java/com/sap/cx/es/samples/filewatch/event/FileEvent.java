/**
 * ***********************************************************************
 * Copyright (c) 2020, SAP <sap.com>
 *
 * All portions of the code written by SAP are property of SAP.
 * All Rights Reserved.
 *
 * SAP
 *
 * Moscow, Russian Federation
 *
 * Web: sap.com
 * ***********************************************************************
 */
package com.sap.cx.es.samples.filewatch.event;

import java.io.File;
import java.util.EventObject;

/**
 *
 *
 * @author Alexei Liubimov <alexei.liubimov@teamidea.ru>
 * @package com.sap.cx.es.samples.filewatch.event
 * @link http://sap.com/
 * @copyright 2020 SAP
 */
public class FileEvent extends EventObject
{
	public FileEvent(File file){
		super(file);
	}

	public File getFile(){
		return (File) getSource();
	}
}
