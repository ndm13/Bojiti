package net.miscfolder.bojiti.task;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TaskTest extends Application{
	final ObservableList<DownloadTask> tasks = FXCollections.observableArrayList();
	final TableView<DownloadTask> table = new TableView<>(tasks);
	{
		table.getColumns().addAll(
				new TableColumn<DownloadTask,String>("Title"){{
					setCellValueFactory(new PropertyValueFactory<>("title"));
				}},
				new TableColumn<DownloadTask,String>("Message"){{
					setCellValueFactory(new PropertyValueFactory<>("message"));
				}},
				new TableColumn<DownloadTask,Double>("Progress"){{
					setCellValueFactory(new PropertyValueFactory<>("progress"));
					setCellFactory(ProgressBarTableCell.forTableColumn());
				}}
		);
	}
	final Button button = new Button("add");
	{
		button.setOnAction(e->{
			try{
				DownloadTask task = new DownloadTask(new URL("http://ipv4.download.thinkbroadband.com:8080/20MB.zip"));
				tasks.add(task);
				ForkJoinPool.commonPool().execute(task);
			}catch(MalformedURLException ignore){}
		});
	}

	public static void main(String[] args){
		launch(args);
	}

	@Override
	public void start(Stage stage){
		VBox box = new VBox();
		box.getChildren().addAll(
				table,
				button
		);

		stage.setScene(new Scene(box));
		stage.show();
	}
}
