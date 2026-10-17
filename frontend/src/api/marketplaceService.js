import axios from './axiosInstance'

export class FeatureUnavailableError extends Error {
  constructor(feature) { super(`${feature} API is not available in the current backend.`); this.name = 'FeatureUnavailableError' }
}

// Interfaces intentionally use only paths present in docs/api-contract.md.
// They are ready to wire when SpaceController and ReservationController are added.
export const workspaceService = {
  async search(params) { return (await axios.get('/api/spaces', { params })).data },
  async getById(id) { return (await axios.get(`/api/spaces/${encodeURIComponent(id)}`)).data },
}

export const bookingService = {
  async create(request) { return (await axios.post('/api/reservations', request)).data },
  async mine(params) { return (await axios.get('/api/reservations/my', { params })).data },
  async cancel(id) { return (await axios.delete(`/api/reservations/${encodeURIComponent(id)}`)).data },
  async forSpace(id, params) { return (await axios.get(`/api/spaces/${encodeURIComponent(id)}/reservations`, { params })).data },
  async updateStatus(id, status) { return (await axios.put(`/api/reservations/${encodeURIComponent(id)}/status`, { status })).data },
}

export const ownerService = {
  async createWorkspace(request) { return (await axios.post('/api/spaces', request)).data },
  async updateWorkspace(id, request) { return (await axios.put(`/api/spaces/${encodeURIComponent(id)}`, request)).data },
  async availability(id, params) { return (await axios.get(`/api/spaces/${encodeURIComponent(id)}/availability`, { params })).data },
  async addBlockedPeriod(id, request) { return (await axios.post(`/api/spaces/${encodeURIComponent(id)}/blocked-periods`, request)).data },
}
