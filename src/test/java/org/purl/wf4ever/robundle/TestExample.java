package org.purl.wf4ever.robundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.NavigableMap;

import org.junit.Test;
import org.purl.wf4ever.robundle.ROBundle;
import org.purl.wf4ever.robundle.ROBundles;

public class TestExample {
	@Test
	public void example() throws Exception {
		// Create a new (temporary) data bundle
		ROBundle dataBundle = ROBundles.createDataBundle();

		// Get the inputs
		Path inputs = ROBundles.getInputs(dataBundle);

		// Get an input port:
		Path portIn1 = ROBundles.getPort(inputs, "in1");

		// Setting a string value for the input port:
		ROBundles.setStringValue(portIn1, "Hello");

		// And retrieving it
		if (ROBundles.isValue(portIn1)) {
			System.out.println(ROBundles.getStringValue(portIn1));
		}

		// Or just use the regular Files methods:
		for (String line : Files
				.readAllLines(portIn1, Charset.forName("UTF-8"))) {
			System.out.println(line);
		}

		// Binaries and large files are done through the Files API
		try (OutputStream out = Files.newOutputStream(portIn1,
				StandardOpenOption.APPEND)) {
			out.write(32);
		}
		// Or Java 7 style
		Path localFile = Files.createTempFile("", ".txt");
		Files.copy(portIn1, localFile, StandardCopyOption.REPLACE_EXISTING);
		System.out.println("Written to: " + localFile);

		// Either way works, of course
		Path outputs = ROBundles.getOutputs(dataBundle);
		Files.copy(localFile,
				ROBundles.getPort(outputs, "out1"));


		// When you get a port, it can become either a value or a list
		Path port2 = ROBundles.getPort(inputs, "port2");
		ROBundles.createList(port2); // empty list		
		if (ROBundles.isList(port2)) {
			List<Path> list = ROBundles.getList(port2);
			assertTrue(list.isEmpty());
		}

		// Adding items sequentially
		Path item0 = ROBundles.newListItem(port2);
		ROBundles.setStringValue(item0, "item 0");
		ROBundles.setStringValue(ROBundles.newListItem(port2), "item 1");
		ROBundles.setStringValue(ROBundles.newListItem(port2), "item 2");
		
		
		// Set and get by explicit position:
		ROBundles.setStringValue(ROBundles.getListItem(port2, 12), "item 12");
		System.out.println(ROBundles.getStringValue(ROBundles.getListItem(port2, 2)));
		
		// The list is sorted numerically (e.g. 2, 5, 10) and
		// will contain nulls for empty slots
		System.out.println(ROBundles.getList(port2));

		// Ports can be browsed as a map by port name
		NavigableMap<String, Path> ports = ROBundles.getPorts(inputs);
		System.out.println(ports.keySet());
		
		// Representing references
		URI ref = URI.create("http://example.com/external.txt");
		Path out3 = ROBundles.getPort(outputs, "out3");
		System.out.println(ROBundles.setReference(out3, ref));
		if (ROBundles.isReference(out3)) {
			URI resolved = ROBundles.getReference(out3);
			System.out.println(resolved);
		}

		
		
		
		// Saving a data bundle:
		Path zip = Files.createTempFile("databundle", ".zip");
		ROBundles.closeAndSaveDataBundle(dataBundle, zip);
		// NOTE: From now dataBundle and its Path's are CLOSED 
		// and can no longer be accessed
		
		
		System.out.println("Saved to " + zip);
		if (Desktop.isDesktopSupported()) {
			// Open ZIP file for browsing
			Desktop.getDesktop().open(zip.toFile());
		}
		
		// Loading a data bundle back from disk
		try (ROBundle dataBundle2 = ROBundles.openDataBundle(zip)) {
			assertEquals(zip, dataBundle2.getSource());
			Path loadedInputs = ROBundles.getInputs(dataBundle2);
			
			for (Path port : ROBundles.getPorts(loadedInputs).values()) {
				if (ROBundles.isValue(port)) {
					System.out.print("Value " + port + ": ");
					System.out.println(ROBundles.getStringValue(port));
				} else if (ROBundles.isList(port)) {
					System.out.print("List " + port + ": ");
					for (Path item : ROBundles.getList(port)) {
						// We'll assume depth 1 here
						System.out.print(ROBundles.getStringValue(item));
						System.out.print(", ");
					}
					System.out.println();
				}				
			}			
		}				
	}
}