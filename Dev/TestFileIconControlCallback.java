package Dev;

import com.liferay.nativity.modules.fileicon.FileIconControlCallback;

import java.util.Random;

/**
 * @author Gail Hernandez
 */
public class TestFileIconControlCallback implements FileIconControlCallback {

	@Override
	public int getIconForFile(String path) {
		return _random.nextInt(10);
	}

	private Random _random = new Random();

}