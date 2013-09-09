package com.wanikani.androidnotifier.db;

import java.io.IOException;
import java.io.OutputStream;

import android.database.SQLException;

/* 
 *  Copyright (c) 2013 Alberto Cuda
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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

/**
 * A database import/export format. Implementations of this class are able to export and parse
 * the full database. Since I want to keep things as simple as possible, they are not meant
 * to be a general-purpose implementations of a data format, and have a knowledge of the data
 * they are exporting/importing. 
 */
public interface Format {
	
	public void export (OutputStream os)
		throws IOException, SQLException;
	
	public String getType ();
}
