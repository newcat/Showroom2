package models;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Freddy on 11.11.2016.
 */

public class ProjectModel {

    private ArrayList<String> members;
    private String title;
    private Department department;
    private String titleImage;
    private Date date;
    private ArrayList<String> tags;
    private ArrayList<BlockModel> blocks;

    public ProjectModel(ArrayList<String> members, String title, Department department,
                        String titleImage, Date date,
                        ArrayList<String> tags, ArrayList<BlockModel> blocks) {

        this.members = members;
        this.title = title;
        this.department = department;
        this.titleImage = titleImage;
        this.date = date;
        this.tags = tags;
        this.blocks = blocks;

    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public String getTitle() {
        return title;
    }

    public Department getDepartment() {
        return department;
    }

    public String getTitleImage() {
        return titleImage;
    }

    public Date getDate() {
        return date;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public ArrayList<BlockModel> getBlocks() {
        return blocks;
    }

}
