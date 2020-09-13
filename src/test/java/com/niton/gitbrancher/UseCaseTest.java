package com.niton.gitbrancher;

import com.niton.gitbrancher.model.exceptions.VersioningException;
import com.niton.gitbrancher.model.versioning.Project;
import com.niton.gitbrancher.model.versioning.ReleaseState;
import com.niton.gitbrancher.model.versioning.ReleaseStep;
import com.niton.gitbrancher.model.versioning.VersionNumber;
import com.niton.media.filesystem.Directory;
import com.niton.media.filesystem.NFile;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class UseCaseTest {
	public static void main(String[] args) throws ClassNotFoundException, GitAPIException, VersioningException, IOException {
		Path repo = Paths.get("D:\\Users\\Nils\\Desktop\\Test\\git-v-manager");
		Project p = Project.init(repo);

		p.getConfig().getReleaseStateUsageMap().put(ReleaseState.PRE_RELEASE,false);
		p.getConfig().getReleaseStateUsageMap().put(ReleaseState.TESTING,false);
		p.getConfig().getReleaseStateUsageMap().put(ReleaseState.BETA,false);

		Git g = Git.open(new File("D:\\Users\\Nils\\Desktop\\Test\\git-v-manager"));

		VersionNumber base = p.nextVersion(p.getCurrentNumber(), ReleaseStep.MINOR);
		commit("basefile",g);
		while(p.getCurrentState() != ReleaseState.RELEASED)
			p.commitCurrentRelease();


		VersionNumber feature1 = p.nextVersion(base,ReleaseStep.PATCH);
		commit("feature1",g);
		while(p.getCurrentState() != ReleaseState.RELEASED)
			p.commitCurrentRelease();

		VersionNumber feature2 = p.nextVersion(feature1, ReleaseStep.PATCH);
		commit("feature2",g);
		p.commitCurrentRelease();

		VersionNumber newBase = p.nextVersion(base, ReleaseStep.MINOR);
		commit("basefileBIG3",g);

		p.switchTo(feature1, ReleaseState.DEVELOPMENT,null);
		commit("fixed Feature1", g);
		while(p.getCurrentState() != ReleaseState.RELEASED)
			p.commitCurrentRelease();

		p.switchTo(base,ReleaseState.DEVELOPMENT,null);
		commit("fixedBasefile", g);
		p.commitCurrentRelease();
	}

	private static void commit(String basefile,Git g) throws GitAPIException, IOException {
		new NFile("D:\\Users\\Nils\\Desktop\\Test\\git-v-manager\\"+basefile).save();
		g.add().addFilepattern(basefile).call();
		g.commit().setMessage("Commiting "+basefile).call();
	}
}
