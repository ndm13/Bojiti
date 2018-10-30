package net.miscfolder.bojiti.mvc.view;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Random;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.miscfolder.bojiti.downloader.Response;

public class JavaFX extends Application{
	private Stage stage;

	private ObservableList<Response> responses = FXCollections.observableArrayList();

	@Override
	public void start(Stage stage){
		setUserAgentStylesheet(STYLESHEET_CASPIAN);
		this.stage = stage;
		stage.setTitle("UI Test");

		BorderPane pane = new BorderPane();
		pane.setBackground(new Background(new BackgroundFill(new Color(.2,.2,.2,1), null, null)));

		pane.setTop(getMenu());
		pane.setLeft(getSidebar());
		pane.setCenter(getMain());

		Scene scene = new Scene(pane, 600, 400);

		stage.setScene(scene);
		stage.show();
	}

	private Node getMain(){
		SplitPane main = new SplitPane();
		main.setOrientation(Orientation.VERTICAL);

		TableView<Response> tableView = new TableView<>(responses);
		tableView.setOnMouseClicked(event -> {
			if(event.getClickCount()==2){
				try{
					Desktop.getDesktop().browse(
							tableView.getSelectionModel().getSelectedItem().getURL().toURI());
				}catch(URISyntaxException | IOException e){
					e.printStackTrace();
				}
			}
		});

		main.getItems().add(tableView);

		TextFlowLog log = new TextFlowLog();
		log.getScrollPane()
				.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		log.getScrollPane()
				.setPadding(new Insets(5));

		main.getItems().add(log.getScrollPane());

		System.setOut(log.createPrintStream(Color.BLACK));
		System.setErr(log.createPrintStream(Color.RED));

		System.out.println("Standard output text.");
		Thread.dumpStack();

		return main;
	}

	private Node getSidebar(){
		VBox sidebar = new VBox();

		Button yelp = new Button("Yelp!");
		yelp.setOnAction(action->{
			System.out.println("Yelp!");
		});
		sidebar.getChildren().add(yelp);

		Button dump = new Button("Dump");
		dump.setOnAction(action->{
			Thread.dumpStack();
		});
		sidebar.getChildren().add(dump);

		Button add = new Button("Add");
		add.setOnAction(action->{
			try{
				Response response = new Response(new URL("http://www.google.com/"),
						Charset.defaultCharset(),
						"text/html",
						10000000);
				responses.add(response);
				new Thread(()->{
					try{
						int length = 10000000, done = 0;
						Random random = new Random();
						while(done < length){
							int round = random.nextInt(length - done);
							Thread.sleep(1000);
							response.updateSpeed(round, 1000);
							done += round;
							System.out.println(done + " " + length);
						}
					}catch(InterruptedException ignore){}
				}).start();
			}catch(MalformedURLException ignore){}
		});
		sidebar.getChildren().add(add);

		sidebar.getChildren().forEach(n->((Region)n).setPrefWidth(100));
		return sidebar;
	}

	private Node getMenu(){
		MenuBar menu = new MenuBar();

		Menu file = new Menu("File");
		MenuItem exit = new MenuItem("Exit");
		exit.setAccelerator(new KeyCodeCombination(KeyCode.F4, KeyCombination.ALT_DOWN));
		exit.setOnAction(e->this.stage.close());
		file.getItems().add(exit);

		menu.getMenus().add(file);

		return menu;
	}

	public static void main(String... args){
		launch();
	}

}
