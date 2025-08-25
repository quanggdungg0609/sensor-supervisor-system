
export interface Device {
  device_uuid: string;
  device_name: string;
  mqtt_username: string;
  client_id: string;
}

export interface DeviceListResponse {
  data: Device[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
  first: boolean;
  last: boolean;
  has_next: boolean;
  has_previous: boolean;
}
