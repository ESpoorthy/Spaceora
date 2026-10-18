import axios from './axiosInstance'

export class FeatureUnavailableError extends Error {
  constructor(feature) { super(`${feature} API is not available in the current backend.`); this.name = 'FeatureUnavailableError' }
}

// Placeholder image used when a space has no photo URL.
const PLACEHOLDER_IMAGE = '/images/space-01.jpeg'

const SPACE_TYPE_BADGE = {
  COWORKING: 'COWORKING DESK',
  STUDY_ROOM: 'STUDY ROOM',
  MEETING_ROOM: 'MEETING ROOM',
  TRAINING_HALL: 'TRAINING HALL',
}

// Derive a short locality label from an address string. Seed addresses are
// formatted "street, area, city postcode", so the second segment reads as a
// neighbourhood; fall back to the first segment, then the city.
function deriveArea(address, city) {
  if (address) {
    const parts = address.split(',').map((s) => s.trim()).filter(Boolean)
    if (parts[1]) return parts[1]
    if (parts[0]) return parts[0]
  }
  return city || ''
}

function toBadge(type) {
  return SPACE_TYPE_BADGE[type] || 'SPACEORA'
}

// Adapt a backend SpaceSummaryDto (GET /api/spaces list item) into the
// view-model shape the UI components expect. Summary payloads carry no
// address, so cards render the city only.
export function mapSpaceSummary(dto) {
  return {
    id: dto.id,
    name: dto.name,
    type: dto.type,
    city: dto.city,
    area: '',
    price: Number(dto.hourlyRate) || 0,
    hourlyRate: Number(dto.hourlyRate) || 0,
    capacity: dto.capacity,
    image: dto.thumbnailUrl || PLACEHOLDER_IMAGE,
    amenities: [],
    badge: toBadge(dto.type),
    isActive: dto.isActive,
  }
}

// Adapt a backend SpaceDto (GET /api/spaces/{id}) into the view-model shape
// the detail page expects.
export function mapSpaceDetail(dto) {
  const images = Array.isArray(dto.images) ? dto.images : []
  const imageAt = (i) => images[i]?.imageUrl || PLACEHOLDER_IMAGE
  return {
    id: dto.id,
    ownerId: dto.ownerId,
    name: dto.name,
    type: dto.type,
    city: dto.city,
    address: dto.address,
    area: deriveArea(dto.address, dto.city),
    description: dto.description || '',
    price: Number(dto.hourlyRate) || 0,
    hourlyRate: Number(dto.hourlyRate) || 0,
    dailyRate: dto.dailyRate != null ? Number(dto.dailyRate) : null,
    capacity: dto.capacity,
    image: imageAt(0),
    image2: imageAt(1),
    image3: imageAt(2),
    amenities: [],
    badge: toBadge(dto.type),
    isActive: dto.isActive,
    availabilitySchedule: Array.isArray(dto.availabilitySchedule) ? dto.availabilitySchedule : [],
    createdAt: dto.createdAt,
  }
}

// Interfaces intentionally use only paths present in docs/api-contract.md.
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
