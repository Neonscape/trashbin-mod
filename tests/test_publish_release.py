"""Exercise release failure/retry behavior without contacting GitHub."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/publish_release.sh"
BASH = (r"C:\Program Files\Git\bin\bash.exe" if os.name == "nt" else shutil.which("bash"))

# A shell function replaces only gh; checksum verification runs for real.
MOCK_GH = r'''
gh() {
    printf '%s\0' "$@" >> "$CALL_LOG"
    printf '\n' >> "$CALL_LOG"
    case "$1 $2" in
        'release view')
            case "$RELEASE_STATE" in
                missing) return 1 ;;
                draft) echo true ;;
                published) echo false ;;
            esac ;;
        'release create'|'release upload')
            if [[ "$FAIL_UPLOAD" == 1 ]]; then return 1; fi ;;
    esac
}
export -f gh
bash "$PUBLISH_SCRIPT"
'''


@unittest.skipUnless(BASH and Path(BASH).is_file(), "bash is required")
class PublishReleaseTests(unittest.TestCase):
    def setUp(self):
        self.workspace = tempfile.TemporaryDirectory()
        self.addCleanup(self.workspace.cleanup)
        self.root = Path(self.workspace.name)
        dist = self.root / "dist"
        dist.mkdir()
        checksums = []
        for mc in ("1.18.2", "1.19.2", "1.20.1"):
            jar = dist / f"trashbin-forge-{mc}-1.0.0.jar"
            jar.write_bytes(f"test jar for {mc}".encode())
            checksums.append(f"{hashlib.sha256(jar.read_bytes()).hexdigest()}  {jar.name}")
        (dist / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n", encoding="utf-8", newline="\n")
        self.log = self.root / "gh.log"

    def run_release(self, state="missing", fail_upload=False, missing_token=False):
        env = dict(os.environ, GH_TOKEN="test-token", GH_REPO="test/trashbin",
                   RELEASE_TAG="v1.0.0", RELEASE_STATE=state,
                   FAIL_UPLOAD=str(int(fail_upload)), CALL_LOG=self.log.as_posix(),
                   PUBLISH_SCRIPT=SCRIPT.as_posix())
        if missing_token:
            del env["GH_TOKEN"]
        result = subprocess.run([BASH, "-c", MOCK_GH], cwd=self.root, env=env,
                                capture_output=True, text=True)
        calls = []
        if self.log.exists():
            calls = [line.rstrip("\0").split("\0")
                     for line in self.log.read_text().splitlines()]
        return result, calls

    def test_new_release_is_draft_until_assets_are_uploaded(self):
        result, calls = self.run_release()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([call[1] for call in calls], ["view", "create", "edit"])
        self.assertIn("--verify-tag", calls[1])
        self.assertIn("--draft", calls[1])
        self.assertEqual(sum(arg.endswith(".jar") for arg in calls[1]), 3)
        self.assertIn("dist/SHA256SUMS.txt", calls[1])
        self.assertIn("--draft=false", calls[2])

    def test_retry_resumes_existing_draft(self):
        result, calls = self.run_release(state="draft")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([call[1] for call in calls], ["view", "upload", "edit"])
        self.assertIn("--clobber", calls[1])

    def test_published_release_is_unchanged(self):
        result, calls = self.run_release(state="published")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([call[1] for call in calls], ["view"])

    def test_failed_new_upload_does_not_publish(self):
        result, calls = self.run_release(fail_upload=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual([call[1] for call in calls], ["view", "create"])

    def test_failed_draft_upload_does_not_publish(self):
        result, calls = self.run_release(state="draft", fail_upload=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual([call[1] for call in calls], ["view", "upload"])

    def test_corrupt_artifact_is_rejected_before_any_github_request(self):
        next((self.root / "dist").glob("*.jar")).write_bytes(b"corrupt")
        result, calls = self.run_release()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, [])

    def test_missing_credentials_are_rejected_before_any_github_request(self):
        result, calls = self.run_release(missing_token=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, [])


if __name__ == "__main__":
    unittest.main()
