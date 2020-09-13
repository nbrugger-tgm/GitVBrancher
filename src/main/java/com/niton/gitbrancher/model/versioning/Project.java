package com.niton.gitbrancher.model.versioning;

import com.google.gson.Gson;
import com.niton.gitbrancher.model.OOSSerializer;
import com.niton.gitbrancher.model.exceptions.VersioningException;
import com.niton.media.filesystem.Directory;
import com.niton.media.filesystem.NFile;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.niton.gitbrancher.model.versioning.ReleaseState.RELEASED;

public class Project implements Serializable {
	private static String databaseName = "versions";
	private static String folderName = "versioning";
	private final List<VersionNumber> versions;
	private final Map<VersionNumber,ArrayList<VersionNumber>> basedOfMap;
	private VersioningConfig config;
	private VersionNumber currentNumber;
	private ReleaseState currentState;
	private String currentExtension;

	private transient DB index;
	private transient Git git;
	private transient NFile configFile;
	private transient NFile stateFile;
	private transient Gson gson = new Gson();


	private Project(DB progressDB, Git git, NFile configFile, NFile stateFile) throws VersioningException, IOException, ClassNotFoundException, GitAPIException {
		this.configFile = configFile;
		this.stateFile = stateFile;
		this.index = progressDB;
		this.git = git;

		versions = index.indexTreeList("versions", new OOSSerializer<VersionNumber>()).createOrOpen();
		basedOfMap = index.hashMap("dependencies",new OOSSerializer<VersionNumber>(),new OOSSerializer<ArrayList<VersionNumber>>()).createOrOpen();
		if(configFile.exisits())
			config = gson.fromJson(configFile.getText(), VersioningConfig.class);
		else {
			config = new VersioningConfig();
			writeConfig();
		}

		if(stateFile.exisits()){
			readState();
		}else {
			Map<ReleaseStep, Integer> initVersion = new HashMap<>();
			Arrays.stream(config.getNumberSchema().getReleaseChunks()).forEach(e -> initVersion.put(e, 0));
			currentNumber = new VersionNumber(initVersion);

			currentState = RELEASED;
			while (!config.getReleaseStateUsageMap().get(currentState)) {
				currentState = currentState.getMergeTarget();
				if (currentState == null) {
					throw new VersioningException("There was no enables ReleaseState (Development,Beta,Release etc)");
				}
			}
			currentExtension = null;
			git.branchCreate().setName(getCurrentNotation()).call();
			switchTo(currentNumber, currentState, currentExtension);
		}

	}

	public static boolean isReady(Path p) throws IOException {
		Path parent = p;
		if((parent = Files.list(parent).filter(e -> e.toFile().getName().equals(".git")).findAny().orElse(null)) != null)
			if((parent = Files.list(parent).filter(e -> e.toFile().getName().equals(folderName)).findAny().orElse(null)) != null)
				return Files.list(parent).filter(e -> e.toFile().getName().equals(databaseName)).findAny().orElse(null) != null;
		return false;
	}

	public static Project init(Path folder) throws IOException, GitAPIException, ClassNotFoundException, VersioningException {
		if(isReady(folder))
			return readFrom(folder);
		Directory root = new Directory(folder);
		Git git;
		if(Files.exists(root.getPath())){
			root = root.addDir(".git");
			if(Files.exists(root.getPath())){
				git = Git.open(root.getParent().getPath().toFile());
				root = root.addDir(folderName);
			}
			else{
				git = Git.init().setDirectory(root.getParent().getPath().toFile()).setGitDir(root.getPath().toFile()).call();
				NFile readme = new NFile(root.getParent(),"README.md");
				readme.save();
				git.add().addFilepattern("README.md").call();
				git.commit().setMessage("init").call();
				root = root.addAndSaveDir(folderName);
			}
		}else{
			root.save();
			git = Git.init().setDirectory(root.getPath().toFile()).setGitDir(root.addDir(".git").getPath().toFile()).call();
			NFile readme = new NFile(root.getParent(),"README.md");
			readme.save();
			git.add().addFilepattern("README.md").call();
			git.commit().setMessage("init").call();
			root = root.addDir(".git").addAndSaveDir(folderName);
		}
		Project p = new Project(
			DBMaker.fileDB(root.addFile(databaseName).toFile()).closeOnJvmShutdown().transactionEnable().make(),
			git,
				root.addFile("config","json"), root.addFile("state"));
		return p;
	}

	private static Project readFrom(Path path) throws IOException, ClassNotFoundException, VersioningException, GitAPIException {
		Directory root = new Directory(path);
		Git git;
		git = Git.open(root.getPath().toFile());
		root = root.addDir(".git").addDir(folderName);

		Project p = new Project(
			DBMaker.fileDB(root.addFile(databaseName).toFile()).closeOnJvmShutdown().transactionEnable().make(),
			git,root.addFile("config","json"), root.addFile("state"));
		return p;
	}

	public void commitCurrentRelease() throws VersioningException, GitAPIException, IOException {
		if(!isStashEmpty())
			throw new VersioningException("Commit all changes bevore commiting the Release");

		if(!isCorrectBranch())
			throw new VersioningException("The current branch doesnt reflects the current version, please switch to the branch '" +getCurrentNotation()+"'");
		if(currentState.getMergeTarget() == null)
			throw new VersioningException("You cannot further release a "+currentState+" ("+config.getReplacement(currentState)+") version as this is the final form");
		ReleaseState targetState = currentState.getMergeTarget();
		while(!config.getReleaseStateUsageMap().get(targetState)) {
			targetState = targetState.getMergeTarget();
			if (targetState == null)
				throw new VersioningException("There is no allowed state to release to, please enable the RELEASED state");
		}

		String newNotation = getVersionNotation(currentNumber,targetState,currentExtension);
		Ref fromBranch = git.getRepository().findRef(getCurrentNotation());
		Ref releasedBranch = git.getRepository().findRef(newNotation);
		if(releasedBranch != null)
			releasedBranch = git.checkout().setName(newNotation).call();
		else
			releasedBranch = git.checkout().setCreateBranch(true).setName(newNotation).call();
		git.merge()
			.include(fromBranch)
			.setCommit(true)
			.setMessage("Release from "+ currentState+" -> "+targetState)
			.call();
		switchTo(currentNumber,targetState,currentExtension);
		if(currentState == RELEASED) {
			if(git.getRepository().findRef(getCurrentNotation()) == null)
				git.tag().setName("v"+getCurrentNotation()).call();
			else
				git.tag().setName("v"+getCurrentNotation()).setForceUpdate(true).call();
			updateOn(currentNumber, currentExtension);
		}
	}

	private void updateOn(VersionNumber number, String extension) throws GitAPIException, IOException, VersioningException {
		Ref toMerge = git.getRepository().findRef(getVersionNotation(number, RELEASED,extension));
		if(toMerge == null)
			throw new VersioningException("Cannot find release Branch to update from");
		if(basedOfMap.containsKey(number)){
			for (VersionNumber dependentVersion : basedOfMap.get(number)) {
				boolean wasReleaseUpdated = false;
				for (Map.Entry<ReleaseState, Boolean> stateInUsed : config.getReleaseStateUsageMap().entrySet()) {
					if(!stateInUsed.getValue()) continue;
					String targetNotation = getVersionNotation(dependentVersion,stateInUsed.getKey(),extension);
					if(git.getRepository().findRef(targetNotation) == null)
						continue;
					git.checkout().setCreateBranch(false).setName(targetNotation).call();
					git.merge().include(toMerge).setMessage("Update from base version").call();
					if(stateInUsed.getKey() == RELEASED){
						if(git.getRepository().findRef(targetNotation) == null)
							git.tag().setName("v"+targetNotation).call();
						else
							git.tag().setName("v"+targetNotation).setForceUpdate(true).call();
					}
					wasReleaseUpdated |= stateInUsed.getKey() == RELEASED;
				}
				if(wasReleaseUpdated)
					updateOn(dependentVersion, extension);
			}
			git.checkout().setName(getCurrentNotation()).call();
		}
	}

	private boolean isCorrectBranch() throws IOException {
		return git.getRepository().getBranch().equals(getVersionNotation(currentNumber,currentState,currentExtension));
	}

	public boolean isStashEmpty() throws GitAPIException {
		return
				git.status().call().getUncommittedChanges().size() == 0 &&
						git.stashList().call().isEmpty() &&
						git.diff().setCached(true).setShowNameAndStatusOnly(true).call().size() == 0;
	}

	public VersionNumber getCurrentNumber() {
		return currentNumber;
	}

	private void writeState() throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(stateFile.getOutputStream());
		oos.writeObject(currentNumber);
		oos.writeObject(currentState);
		oos.writeObject(currentExtension);
		oos.close();
	}

	private void readState() throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(stateFile.getInputStream());
		currentNumber = (VersionNumber) ois.readObject();
		currentState = (ReleaseState) ois.readObject();
		currentExtension = (String) ois.readObject();
		ois.close();
	}

	private void writeConfig() throws IOException {
		configFile.setText(gson.toJson(config));
	}

	public VersioningConfig getConfig() {
		return config;
	}
	public String getCurrentNotation(){
		return getVersionNotation(currentNumber,currentState,currentExtension);
	}
	public String getVersionNotation(VersionNumber number,ReleaseState state,String extension) {
		final StringBuilder sb = new StringBuilder();
		if(config.getDifferenciation() != null){
			sb.append(extension);
			sb.append(VersioningConfig.differencingSeperator);
		}
		sb.append(
				number.getNotation(config)
		);
		if(state != RELEASED) {
			sb.append(VersioningConfig.stateSeperator);
			sb.append(config.getReleaseStateReplacementMap().get(state));
		}
		return sb.toString();
	}
	public VersionNumber nextVersion(VersionNumber base,ReleaseStep size) throws GitAPIException, IOException, VersioningException {

		switchTo(base,RELEASED,null);

		VersionNumber newNumber = new VersionNumber();
		//overtake superior
		ReleaseStep above = size.getSuperior();
		while(above != null){
			newNumber.setNumber(above, base.getNumber(above));
			above = above.getSuperior();
		}
		//increase
		newNumber.setNumber(size,base.getNumber(size)+1);
		//set bellow to 0
		for(ReleaseStep r : ReleaseStep.values()){
			if(newNumber.getNumber(r) == null)
				newNumber.setNumber(r,0);
		}
		versions.add(newNumber);

		ArrayList<VersionNumber> bases = basedOfMap.getOrDefault(base, new ArrayList<>(1));
		bases.add(newNumber);
		basedOfMap.put(base,bases);

		ReleaseState newState = ReleaseState.getStartingState();
		while(!config.getReleaseStateUsageMap().get(newState))
			newState = newState.getMergeTarget();
		git.branchCreate().setName(getVersionNotation(newNumber, newState, null)).call();
		switchTo(newNumber, newState, null);
		return newNumber;
	}
	public void switchTo(VersionNumber number,ReleaseState state,String extension) throws GitAPIException, IOException, VersioningException {
		currentNumber = number;
		currentState = state;
		currentExtension = extension;
		try {
			git.checkout().setName(getCurrentNotation()).call();
		}catch (GitAPIException e){
			if(e.getMessage().endsWith("cannot be resolved")){
				throw new VersioningException("Cant switch to non existing version ("+getCurrentNotation()+"). You maybe need to release it first.");
			}else
				throw e;
		}
		writeState();
	}

	public ReleaseState getCurrentState() {
		return currentState;
	}
}
