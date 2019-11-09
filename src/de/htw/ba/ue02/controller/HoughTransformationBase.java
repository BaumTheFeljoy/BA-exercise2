/**
 * @author Nico Hezel
 */
package de.htw.ba.ue02.controller;

import java.io.File;

import de.htw.ba.ue02.controller.HoughTransformationController.Methods;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;


public abstract class HoughTransformationBase {
		
	private final static int accWidth = 360;
	private final static int accHeight = 500;
	
	protected static enum Images {
		Linien, Rauschen, Punkt
	};
	
	@FXML
	private ImageView leftImageView;
	
	@FXML
	private ImageView rightImageView;
	
	@FXML
	private ComboBox<Methods> methodSelection;
	
	@FXML
	private ComboBox<Images> imageSelection;
	
	@FXML
	private Label runtimeLabel;
		
	@FXML
	public void initialize() {
		methodSelection.getItems().addAll(Methods.values());
		methodSelection.setValue(Methods.Leer);
		methodSelection.setOnAction(this::applyTransformation);
		
		imageSelection.getItems().addAll(Images.values());
		imageSelection.setValue(Images.Linien);
		imageSelection.setOnAction(this::loadImage);
		
		loadImage(new File("linien.png"));
	}
	
	private int[] originalPixels = null;


	@FXML
	public void onOpenFileClick() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(".")); 
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images (*.jpg, *.png, *.gif)", "*.jpeg", "*.jpg", "*.png", "*.gif"));
		loadImage(fileChooser.showOpenDialog(null));
	}
	
	public void loadImage(ActionEvent event) {
		Images imageName = imageSelection.getSelectionModel().getSelectedItem();
		loadImage(new File(""+imageName.toString().toLowerCase()+".png"));		
	}
	
	public void loadImage(File file) {
		if(file != null) {
			leftImageView.setImage(new Image(file.toURI().toString()));
			originalPixels = imageToPixel(leftImageView.getImage());
			applyTransformation(null);
		}		
	}
	
	public int[] imageToPixel(Image image) {
		int width = (int)image.getWidth();
		int height = (int)image.getHeight();
		int[] pixels = new int[width * height];
		image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		return pixels;
	}
	
	public Image pixelToImage(int[] pixels, int width, int height) {
		WritableImage wr = new WritableImage(width, height);
		PixelWriter pw = wr.getPixelWriter();
		pw.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		return wr;
	}
	
	public abstract void runMethod(Methods currentMethod, int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) throws Exception;
	
	@FXML
	public void applyTransformation(ActionEvent event) {

		// no images loaded
		if(leftImageView.getImage() == null)
			return;
		
	  	// get image dimensions
    	int width = (int)leftImageView.getImage().getWidth();
    	int height = (int)leftImageView.getImage().getHeight();

    	// get pixels arrays
    	int[] srcPixels = originalPixels.clone();
    	int[] dstPixels = new int[accWidth * accHeight];
    	
		long startTime = System.currentTimeMillis();

		// get method choice 
		Methods currentMethod = methodSelection.getSelectionModel().getSelectedItem();
		try {
			runMethod(currentMethod, srcPixels, width, height, dstPixels, accWidth, accHeight);
		} catch (Exception e) { e.printStackTrace();
			e.printStackTrace();
		}
		
		rightImageView.setImage(pixelToImage(dstPixels, accWidth, accHeight));
		leftImageView.setImage(pixelToImage(srcPixels, width, height));
    	runtimeLabel.setText("Methode " + currentMethod + " ausgeführt in " + (System.currentTimeMillis() - startTime) + " ms");
	}

}
