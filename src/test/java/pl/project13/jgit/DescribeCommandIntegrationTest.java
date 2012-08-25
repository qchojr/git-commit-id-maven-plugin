/*
 * This file is part of git-commit-id-plugin by Konrad Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.jgit;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.FileSystemMavenSandbox;
import pl.project13.maven.git.GitIntegrationTest;
import pl.project13.test.utils.AssertException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DescribeCommandIntegrationTest extends GitIntegrationTest {

  public static final int DEFAULT_ABBREV_LEN = 7;
  final String PROJECT_NAME = "my-jar-project";

  @Override
  protected Optional<String> projectDir() {
    return Optional.of(PROJECT_NAME);
  }

  @Test
  public void shouldGiveTheCommitIdAndDirtyMarkerWhenNothingElseCanBeFound() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT_DIRTY)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();

    // when
    DescribeResult res = DescribeCommand
        .on(repo)
        .setVerbose(true)
        .call();

    // then
    assertThat(res).isNotNull();

    RevCommit HEAD = git().log().call().iterator().next();
    assertThat(res.toString()).isEqualTo(abbrev(HEAD.getName()) + "-DEV");
  }

  @Test
  public void shouldGiveTheCommitIdWhenNothingElseCanBeFound() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();

    // when
    DescribeCommand command = spy(DescribeCommand.on(repo));
    doReturn(false).when(command).findDirtyState(any(Repository.class));

    command.setVerbose(true);
    DescribeResult res = command.call();

    // then
    assertThat(res).isNotNull();

    RevCommit HEAD = git().log().call().iterator().next();
    assertThat(res.toString()).isEqualTo(abbrev(HEAD.getName()));
  }


  @Test
  public void shouldGiveTheCommitIdWhenNothingElseCanBeFoundAndUseAbbrevVersionOfIt() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    int abbrevLength = 10;
    Repository repo = git().getRepository();

    // when
    DescribeCommand command = spy(DescribeCommand.on(repo));
    doReturn(false).when(command).findDirtyState(any(Repository.class));

    command
        .setVerbose(true)
        .abbrev(abbrevLength);
    DescribeResult res = command.call();

    // then
    assertThat(res).isNotNull();

    RevCommit HEAD = git().log().call().iterator().next();
    assertThat(res.toString()).isEqualTo(abbrev(HEAD.getName(), abbrevLength));
  }

  @Test
  public void shouldGiveTagWithDistanceToCurrentCommitAndItsIdAndDirtyMarker() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.GIT_COMMIT_ID)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();

    // when
    DescribeCommand command = DescribeCommand.on(repo);
    command.setVerbose(true);
    DescribeResult res = command.call();

    // then
    assertThat(res).isNotNull();
    RevCommit HEAD = git().log().call().iterator().next();
    assertThat(res.toString()).isEqualTo("v2.0.4-25-g" + abbrev(HEAD.getName()) + "-DEV");
  }

  @Test
  public void shouldGiveTagWithDistanceToCurrentCommitAndItsId() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.GIT_COMMIT_ID)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();
    Git.wrap(repo).reset().setMode(ResetCommand.ResetType.HARD).call();

    // when
    DescribeCommand command = DescribeCommand.on(repo);
    command.setVerbose(true);
    DescribeResult res = command.call();

    // then
    assertThat(res).isNotNull();
    RevCommit HEAD = git().log().call().iterator().next();
    assertThat(res.toString()).isEqualTo("v2.0.4-25-g" + abbrev(HEAD.getName()));
  }

  @Test
  public void shouldGiveTag() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();
    git().reset().setMode(ResetCommand.ResetType.HARD).call();

    // when
    DescribeCommand command = DescribeCommand.on(repo);
    command.setVerbose(true);
    DescribeResult res = command.call();

    // then
    assertThat(res).isNotNull();

    assertThat(res.toString()).isEqualTo("v1.0.0");
  }

  @Test
  public void shouldGiveDirtyTag() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();

    // when
    DescribeCommand command = DescribeCommand.on(repo);
    command.setVerbose(true);
    DescribeResult res = command.call();

    // then
    assertThat(res.toString()).isEqualTo("v1.0.0-DEV");
  }

  @Test
  public void shouldGiveAnnotatedTagWithDirtyMarker() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_ANNOT_TAG_DIRTY)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();
    git().reset().setMode(ResetCommand.ResetType.HARD).call();

    // when
    DescribeCommand command = DescribeCommand.on(repo);
    command.setVerbose(true);
    DescribeResult res = command.call();

    // then
    assertThat(res).isNotNull();

    assertThat(res.toString()).isEqualTo("v1.0.0");
  }

  @Test
  public void shouldGiveLightweightTagWithDirtyMarker() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG_DIRTY)
        .create(FileSystemMavenSandbox.CleanUp.CLEANUP_FIRST);

    Repository repo = git().getRepository();

    Git.wrap(repo).reset().setMode(ResetCommand.ResetType.HARD).call();

    // when
    DescribeCommand command = DescribeCommand.on(repo);
    command.setVerbose(true);
    DescribeResult res = command.call();

    // then
    assertThat(res).isNotNull();

    assertThat(res.toString()).isEqualTo("v1.0.0");
  }

  @Test
  public void isATag_shouldProperlyDetectIfACommitIsATag() throws Exception {
    // given
    String tagName = "v1";
    String commitHash = "de4db35917b268089c81c9ab1b52541bb778f5a0";

    ObjectId oid = ObjectId.fromString(commitHash);

    // when
    boolean isATag = DescribeCommand.isATag(oid, ImmutableMap.of(oid, tagName));

    // then
    assertThat(isATag).isTrue();
  }

  @Test
  public void isATag_shouldProperlyDetectIfACommitIsANotTag() throws Exception {
    // given
    String tagName = "v1";
    String tagHash = "de4db35917b268089c81c9ab1b52541bb778f5a0";
    ObjectId tagOid = ObjectId.fromString(tagHash);

    String commitHash = "de4db35917b268089c81c9ab1b52541bb778f5a0";
    ObjectId oid = ObjectId.fromString(commitHash);

    // when
    boolean isATag = DescribeCommand.isATag(oid, ImmutableMap.of(tagOid, tagName));

    // then
    assertThat(isATag).isTrue();
  }

  @Test
  public void trimFullTagName_shouldTrimFullTagNamePrefix() throws Exception {
    // given
    String fullName = "refs/tags/v1.0.0";

    // when
    String simpleName = DescribeCommand.trimFullTagName(fullName);

    // then
    assertThat(simpleName).isEqualTo("v1.0.0");
  }

  @Test
  public void abbrev_shouldVerifyLengthContract_failOn41() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = 41;

    // when
    Runnable block = new Runnable() {
      @Override
      public void run() {
        DescribeCommand.on(repo).abbrev(length);
      }
    };

    // then
    AssertException.thrown(IllegalArgumentException.class, block);
  }

  @Test
  public void abbrev_shouldVerifyLengthContract_failOnMinus12() throws Exception {
    // given
    final Repository repo = mock(Repository.class);
    final int length = -12;

    // when
    Runnable block = new Runnable() {
      @Override
      public void run() {
        DescribeCommand.on(repo).abbrev(length);
      }
    };

    // then
    AssertException.thrown(IllegalArgumentException.class, block);
  }

  String abbrev(String id) {
    return abbrev(id, DEFAULT_ABBREV_LEN);
  }

  String abbrev(String id, int n) {
    return id.substring(0, n);
  }
}