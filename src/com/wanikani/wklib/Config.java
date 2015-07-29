package com.wanikani.wklib;

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

public class Config {
	
	public static final String DEF_URL = "http://www.wanikani.com/api/v1.4"; 

	public static final String DEF_URL_S = "https://www.wanikani.com/api/v1.4"; 

	public static final String DEF_GRAVATAR_URL = "http://www.gravatar.com/avatar"; 
	
	public String url = DEF_URL;
	
	public String gravatarUrl = DEF_GRAVATAR_URL;
	
	public static final Config DEFAULT_TCP =
			new Config ();
	
	public static final Config DEFAULT_TLS =
			new Config (DEF_URL_S, DEF_GRAVATAR_URL);

	public Config ()
	{
		/* empty */		
	}
	
	public Config (String url, String gravatarUrl)
	{
		this.url = url;
		this.gravatarUrl = gravatarUrl;
	}
}
