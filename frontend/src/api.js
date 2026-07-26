async function request(path, options = {}) {
  const response = await fetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json") ? await response.json() : null;

  if (!response.ok) {
    const error = new Error(body?.message || "요청을 처리하지 못했습니다.");
    error.code = body?.code || "REQUEST_FAILED";
    error.status = response.status;
    throw error;
  }

  return body;
}

export function getPolicies() {
  return request("/api/extensions");
}

export function addCustomExtension(extension) {
  return request("/api/extensions/custom", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ extension }),
  });
}

export function updateBlocked(extension, blocked) {
  return request(`/api/extensions/${encodeURIComponent(extension)}/blocked`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ blocked }),
  });
}

export function deleteCustomExtension(id) {
  return request(`/api/extensions/${id}`, { method: "DELETE" });
}

export function getStoredFiles() {
  return request("/api/files");
}

export function uploadFile(file) {
  const formData = new FormData();
  formData.append("file", file);
  return request("/api/files", {
    method: "POST",
    body: formData,
  });
}

export async function downloadStoredFile(id, originalFilename) {
  const response = await fetch(`/api/files/${id}/download`);
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const error = new Error(body?.message || "파일을 다운로드하지 못했습니다.");
    error.code = body?.code || "DOWNLOAD_FAILED";
    throw error;
  }

  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = originalFilename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(objectUrl);
}
