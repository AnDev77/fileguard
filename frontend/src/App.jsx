import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  CheckCircle2,
  Download,
  File,
  FileUp,
  Plus,
  RefreshCw,
  ShieldCheck,
  Trash2,
  UploadCloud,
  XCircle,
} from "lucide-react";
import {
  addCustomExtension,
  deleteCustomExtension,
  downloadStoredFile,
  getPolicies,
  getStoredFiles,
  updateBlocked,
  uploadFile,
} from "./api";

const SYNC_INTERVAL_MS = 5000;

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function getFileExtension(filename) {
  const lastDot = filename.lastIndexOf(".");
  return lastDot >= 0 && lastDot < filename.length - 1
    ? filename.slice(lastDot + 1).toLowerCase()
    : "";
}

function App() {
  const [policies, setPolicies] = useState([]);
  const [files, setFiles] = useState([]);
  const [extension, setExtension] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [busyAction, setBusyAction] = useState("");
  const [lastSyncedAt, setLastSyncedAt] = useState(null);
  const [toast, setToast] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef(null);
  const toastTimerRef = useRef(null);

  const fixedPolicies = useMemo(
    () => policies.filter((policy) => policy.fixed),
    [policies],
  );
  const customPolicies = useMemo(
    () => policies.filter((policy) => !policy.fixed),
    [policies],
  );

  const notify = useCallback((type, title, message) => {
    window.clearTimeout(toastTimerRef.current);
    setToast({ type, title, message });
    toastTimerRef.current = window.setTimeout(() => setToast(null), 4200);
  }, []);

  const refreshData = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setSyncing(true);
    try {
      const [policyResponse, fileResponse] = await Promise.all([
        getPolicies(),
        getStoredFiles(),
      ]);
      setPolicies(policyResponse.data || []);
      setFiles(fileResponse.data || []);
      setLastSyncedAt(new Date());
    } catch (error) {
      if (!silent) notify("error", "동기화 실패", error.message);
    } finally {
      setInitialLoading(false);
      if (!silent) setSyncing(false);
    }
  }, [notify]);

  useEffect(() => {
    refreshData();
    const intervalId = window.setInterval(
      () => refreshData({ silent: true }),
      SYNC_INTERVAL_MS,
    );
    const handleFocus = () => refreshData({ silent: true });
    const handleVisibility = () => {
      if (document.visibilityState === "visible") handleFocus();
    };
    window.addEventListener("focus", handleFocus);
    document.addEventListener("visibilitychange", handleVisibility);

    return () => {
      window.clearInterval(intervalId);
      window.clearTimeout(toastTimerRef.current);
      window.removeEventListener("focus", handleFocus);
      document.removeEventListener("visibilitychange", handleVisibility);
    };
  }, [refreshData]);

  async function handleAddExtension(event) {
    event.preventDefault();
    const value = extension.trim();
    if (!value) {
      notify("error", "입력 확인", "추가할 확장자를 입력하세요.");
      return;
    }

    setBusyAction("add");
    try {
      const response = await addCustomExtension(value);
      setExtension("");
      notify("success", "추가 완료", `.${response.data.extension} 확장자를 차단 목록에 추가했습니다.`);
      await refreshData({ silent: true });
    } catch (error) {
      notify("error", "추가 실패", error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function handleDelete(policy) {
    setBusyAction(`delete-${policy.id}`);
    try {
      await deleteCustomExtension(policy.id);
      notify("success", "삭제 완료", `.${policy.extension} 확장자를 삭제했습니다.`);
      await refreshData({ silent: true });
    } catch (error) {
      notify("error", "삭제 실패", error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function handleToggle(policy) {
    setBusyAction(`toggle-${policy.id}`);
    try {
      await updateBlocked(policy.extension, !policy.blocked);
      notify(
        "success",
        "정책 변경 완료",
        `.${policy.extension} 파일을 ${policy.blocked ? "허용" : "차단"}합니다.`,
      );
      await refreshData({ silent: true });
    } catch (error) {
      notify("error", "정책 변경 실패", error.message);
      await refreshData({ silent: true });
    } finally {
      setBusyAction("");
    }
  }

  function chooseFile(file) {
    if (!file) return;
    setSelectedFile(file);
  }

  async function handleUpload() {
    if (!selectedFile) {
      notify("error", "파일 선택 필요", "업로드할 파일을 선택하세요.");
      return;
    }

    setBusyAction("upload");
    try {
      await uploadFile(selectedFile);
      notify("success", "업로드 완료", `${selectedFile.name} 파일을 저장했습니다.`);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      await refreshData({ silent: true });
    } catch (error) {
      if (error.code === "BLOCKED_FILE_EXTENSION") {
        const fileExtension = getFileExtension(selectedFile.name);
        notify(
          "error",
          "차단된 확장자",
          `.${fileExtension || "알 수 없음"} 파일은 현재 정책에 의해 업로드할 수 없습니다.`,
        );
      } else {
        notify("error", "업로드 실패", error.message);
      }
    } finally {
      setBusyAction("");
    }
  }

  async function handleDownload(file) {
    setBusyAction(`download-${file.id}`);
    try {
      await downloadStoredFile(file.id, file.originalFilename);
      notify("success", "다운로드 시작", `${file.originalFilename} 파일을 다운로드합니다.`);
    } catch (error) {
      notify("error", "다운로드 실패", error.message);
      await refreshData({ silent: true });
    } finally {
      setBusyAction("");
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <ShieldCheck size={22} />
          </span>
          <div>
            <h1>FileGuard</h1>
            <p>파일 확장자 정책 관리</p>
          </div>
        </div>
        <div className="sync-area">
          <span className="sync-status">
            <span className="status-dot" />
            {lastSyncedAt
              ? `${lastSyncedAt.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })} 동기화`
              : "연결 중"}
          </span>
          <button
            className="icon-button"
            type="button"
            title="데이터 새로고침"
            aria-label="데이터 새로고침"
            disabled={syncing}
            onClick={() => refreshData()}
          >
            <RefreshCw size={18} className={syncing ? "spin" : ""} />
          </button>
        </div>
      </header>

      <main className="workspace">
        <div className="policy-column">
          <section className="panel">
            <div className="panel-heading">
              <div>
                <p className="section-kicker">기본 정책</p>
                <h2>고정 확장자</h2>
              </div>
              <span className="count">{fixedPolicies.length}</span>
            </div>
            <div className="fixed-grid" aria-busy={initialLoading}>
              {fixedPolicies.map((policy) => (
                <label className="extension-check" key={policy.id}>
                  <input
                    type="checkbox"
                    checked={policy.blocked}
                    disabled={busyAction === `toggle-${policy.id}`}
                    onChange={() => handleToggle(policy)}
                  />
                  <span>.{policy.extension}</span>
                </label>
              ))}
              {!initialLoading && fixedPolicies.length === 0 && (
                <p className="empty-copy">고정 확장자를 불러오지 못했습니다.</p>
              )}
            </div>
          </section>

          <section className="panel custom-panel">
            <div className="panel-heading">
              <div>
                <p className="section-kicker">사용자 정책</p>
                <h2>커스텀 확장자</h2>
              </div>
              <span className="count">{customPolicies.length} / 200</span>
            </div>
            <form className="extension-form" onSubmit={handleAddExtension}>
              <div className="input-prefix">
                <span aria-hidden="true">.</span>
                <input
                  aria-label="커스텀 확장자"
                  value={extension}
                  maxLength={20}
                  placeholder="확장자 입력"
                  onChange={(event) => setExtension(event.target.value)}
                />
              </div>
              <button
                className="primary-button compact"
                type="submit"
                disabled={busyAction === "add" || customPolicies.length >= 200}
              >
                <Plus size={17} />
                추가
              </button>
            </form>
            <div className="custom-list">
              {customPolicies.map((policy) => (
                <div className="custom-row" key={policy.id}>
                  <span className="extension-name">.{policy.extension}</span>
                  <span className="blocked-label">차단</span>
                  <button
                    className="icon-button danger"
                    type="button"
                    title={`.${policy.extension} 삭제`}
                    aria-label={`.${policy.extension} 삭제`}
                    disabled={busyAction === `delete-${policy.id}`}
                    onClick={() => handleDelete(policy)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
              {!initialLoading && customPolicies.length === 0 && (
                <div className="empty-state small">
                  <p>등록된 커스텀 확장자가 없습니다.</p>
                </div>
              )}
            </div>
          </section>
        </div>

        <div className="file-column">
          <section className="panel upload-panel">
            <div className="panel-heading">
              <div>
                <p className="section-kicker">파일 검사</p>
                <h2>파일 업로드</h2>
              </div>
              <FileUp size={20} className="heading-icon" />
            </div>
            <div
              className={`drop-zone ${dragActive ? "drag-active" : ""}`}
              onDragEnter={(event) => {
                event.preventDefault();
                setDragActive(true);
              }}
              onDragOver={(event) => event.preventDefault()}
              onDragLeave={(event) => {
                event.preventDefault();
                setDragActive(false);
              }}
              onDrop={(event) => {
                event.preventDefault();
                setDragActive(false);
                chooseFile(event.dataTransfer.files?.[0]);
              }}
            >
              <input
                ref={fileInputRef}
                id="file-input"
                type="file"
                onChange={(event) => chooseFile(event.target.files?.[0])}
              />
              <UploadCloud size={30} aria-hidden="true" />
              <p>{selectedFile ? selectedFile.name : "파일을 끌어놓거나 선택하세요"}</p>
              <span>
                {selectedFile
                  ? formatBytes(selectedFile.size)
                  : "파일당 최대 10MB"}
              </span>
              <label className="secondary-button" htmlFor="file-input">
                파일 선택
              </label>
            </div>
            <button
              className="primary-button upload-button"
              type="button"
              disabled={!selectedFile || busyAction === "upload"}
              onClick={handleUpload}
            >
              <FileUp size={18} />
              {busyAction === "upload" ? "업로드 중" : "업로드"}
            </button>
          </section>

          <section className="panel files-panel">
            <div className="panel-heading">
              <div>
                <p className="section-kicker">저장 완료</p>
                <h2>업로드한 파일</h2>
              </div>
              <span className="count">{files.length}</span>
            </div>
            <div className="file-list" aria-live="polite">
              {files.map((file) => (
                <div className="file-row" key={file.id}>
                  <span className="file-icon" aria-hidden="true">
                    <File size={18} />
                  </span>
                  <div className="file-info">
                    <strong title={file.originalFilename}>{file.originalFilename}</strong>
                    <span>
                      {formatBytes(file.size)} · {formatDate(file.createdAt)}
                    </span>
                  </div>
                  <span className="file-extension">.{file.extension}</span>
                  <button
                    className="icon-button download"
                    type="button"
                    title={`${file.originalFilename} 다운로드`}
                    aria-label={`${file.originalFilename} 다운로드`}
                    disabled={busyAction === `download-${file.id}`}
                    onClick={() => handleDownload(file)}
                  >
                    <Download size={18} />
                  </button>
                </div>
              ))}
              {!initialLoading && files.length === 0 && (
                <div className="empty-state">
                  <File size={26} />
                  <p>저장된 파일이 없습니다.</p>
                </div>
              )}
            </div>
          </section>
        </div>
      </main>

      {toast && (
        <div className={`toast ${toast.type}`} role="status" aria-live="polite">
          {toast.type === "success" ? (
            <CheckCircle2 size={20} />
          ) : (
            <XCircle size={20} />
          )}
          <div>
            <strong>{toast.title}</strong>
            <p>{toast.message}</p>
          </div>
          <button
            className="toast-close"
            type="button"
            aria-label="알림 닫기"
            onClick={() => setToast(null)}
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
}

export default App;
