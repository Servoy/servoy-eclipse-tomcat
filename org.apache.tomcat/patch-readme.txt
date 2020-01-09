with no patch you get a lot of exceptions like:

jan. 09, 2020 4:48:34 P.M. org.apache.tomcat.util.descriptor.DigesterFactory locationFor
WARNING: The XML schema [XMLSchema.dtd] could not be found. This is very likely to break XML validation if XML validation is enabled.

update the DigesterFactor in the  source folder with the one of the new tomcat and apply the patch again:

			// patch start
			if (location == null)
			{
				location = DigesterFactory.class.getResource("resources/" + name);
			}
			// patch end
			
in locationFor() method
